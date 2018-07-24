/*
 * 作者：许恕
 * 时间：2016年5月3日
 * 功能：实现tail 某目录下的所有符合正则条件的文件
 * Email：xvshu1@163.com
 * To detect all files in a folder
 */

package org.apache.flume.source;

import com.dianping.cat.Cat;
import com.dianping.cat.message.Transaction;
import com.el.ump.profiler.Profiler;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDrivenSource;
import org.apache.flume.SystemClock;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.instrumentation.SourceCounter;
import org.apache.flume.source.utils.MsgBuildeJson;
import org.mortbay.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  step：
 *    1,config one path
 *    2,find all file with RegExp
 *    3,tail one children file
 *    4,batch to channal
 *
 *  demo:
 *    demo.sources.s1.type = org.apache.flume.source.ExecTailSource
 *    demo.sources.s1.filepath=/export/home/tomcat/logs/auth.el.net/
 *    demo.sources.s1.filenameRegExp=(.log{1})$
 *    demo.sources.s1.tailing=true
 *    demo.sources.s1.readinterval=300
 *    demo.sources.s1.startAtBeginning=false
 *    demo.sources.s1.restart=true
 */
public class ExecTailSource extends AbstractSource implements EventDrivenSource,
        Configurable {

  private static final Logger logger = LoggerFactory
          .getLogger(ExecTailSource.class);

  private SourceCounter sourceCounter;
  private ExecutorService executor;
  private List<ExecTailRunnable> listRuners;
  private List<Future<?>> listFuture;
  private long restartThrottle;
  private boolean restart = true;
  private boolean logStderr;
  private Integer bufferCount;
  private long batchTimeout;
  private Charset charset;
  private String  basefilepath;
  private String filepath;
  private String filenameRegExp;

  private String apilogs;
  private String operatelogs;
  private String tomcatlogs;

  private List<String> apilogsList = new ArrayList<String>();
  private List<String> operatelogsList = new ArrayList<String>();
  private List<String> tomcatlogsList =  new ArrayList<String>();

  private String tailing;
  private Integer readinterval;
  private boolean startAtBeginning;
  private boolean contextIsJson;
  private String fileWriteJson;
  private Long flushTime;
  private boolean contextIsFlumeLog;
  private String domain;
  private String msgTypeConfig;


  @Override
  public void start() {
    logger.info("=start=> flume tail source start begin time:"+new Date().toString());
    logger.info("ExecTail source starting with filepath:{}", filepath);

    List<String> listFiles = getFileList();
    if(listFiles==null || listFiles.isEmpty()){
      logger.info("The filepath's file not have fiels with filenameRegExp");
      sourceCounter.start();
      super.start();
      return;
    }

    Properties prop=null;

    try{
      prop = new Properties();//属性集合对象
      FileInputStream fis = new FileInputStream(fileWriteJson);//属性文件流
      prop.load(fis);
    }catch(Exception ex){
      logger.error("=ExecTailSource.start=>",ex);
    }



    executor = Executors.newFixedThreadPool(listFiles.size());

    listRuners = new ArrayList<ExecTailRunnable>();
    listFuture = new ArrayList<Future<?>>();

    logger.info("files size is {} ", listFiles.size());
    // FIXME: Use a callback-like executor / future to signal us upon failure.
    for(String oneFilePath : listFiles){
      ExecTailRunnable runner = new ExecTailRunnable(sourceCounter,
              restart, restartThrottle, logStderr, bufferCount, batchTimeout,
              charset,oneFilePath,tailing,readinterval,startAtBeginning,contextIsJson,
              prop,fileWriteJson,flushTime,contextIsFlumeLog,domain);
      listRuners.add(runner);
      Future<?> runnerFuture = executor.submit(runner);
      listFuture.add(runnerFuture);
      logger.info("{} is begin running",oneFilePath);
    }

    /*
     * NB: This comes at the end rather than the beginning of the method because
     * it sets our state to running. We want to make sure the executor is alive
     * and well first.
     */
    sourceCounter.start();
    super.start();
    logger.info("=start=> flume tail source start end time:"+new Date().toString());
    logger.debug("ExecTail source started");
  }

  @Override
  public void stop() {

    logger.info("=stop=> flume tail source stop begin time:"+new Date().toString());
    if(listRuners !=null && !listRuners.isEmpty()){
      for(ExecTailRunnable oneRunner : listRuners){
        if(oneRunner != null) {
          oneRunner.setRestart(false);
          oneRunner.kill();
        }
      }
    }


    if(listFuture !=null && !listFuture.isEmpty()){
      for(Future<?> oneFuture : listFuture){
        if (oneFuture != null) {
          logger.debug("Stopping ExecTail runner");
          oneFuture.cancel(true);
          logger.debug("ExecTail runner stopped");
        }
      }
    }

    executor.shutdown();
    while (!executor.isTerminated()) {
      logger.debug("Waiting for ExecTail executor service to stop");
      try {
        executor.awaitTermination(500, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        logger.error("Interrupted while waiting for ExecTail executor service "
                + "to stop. Just exiting.",e);
        Thread.currentThread().interrupt();
      }
    }




    sourceCounter.stop();
    super.stop();
    logger.info("=stop=> flume tail source stop end time:"+new Date().toString());

  }

  @Override
  public void configure(Context context) {

    basefilepath=context.getString(ExecTailSourceConfigurationConstants.BASE_FILE_PATH,
            ExecTailSourceConfigurationConstants.DEFAULT_BASE_FILE_PATH);

    filepath = context.getString("filepath");
    Preconditions.checkState(filepath != null,
            "The parameter filepath must be specified");
    logger.info("The parameter filepath is {}" ,filepath);

    apilogs = context.getString(ExecTailSourceConfigurationConstants.APILOGS,
            ExecTailSourceConfigurationConstants.DEFAULT_CHILDREN_FILE);

    apilogsList.addAll(Arrays.asList(apilogs.split("\\|")));

    operatelogs = context.getString(ExecTailSourceConfigurationConstants.OPERATELOGS,
            ExecTailSourceConfigurationConstants.DEFAULT_CHILDREN_FILE);
    operatelogsList.addAll(Arrays.asList(operatelogs.split("\\|")));

    tomcatlogs = context.getString(ExecTailSourceConfigurationConstants.TOMCATLOGS,
            ExecTailSourceConfigurationConstants.DEFAULT_CHILDREN_FILE);
    tomcatlogsList.addAll(Arrays.asList(tomcatlogs.split("\\|")));

    filenameRegExp = context.getString("filenameRegExp");
    Preconditions.checkState(filenameRegExp != null,
            "The parameter filenameRegExp must be specified");
    logger.info("The parameter filenameRegExp is {}" ,filenameRegExp);

    msgTypeConfig=context.getString(ExecTailSourceConfigurationConstants.CONFIG_MSGTYPECONFIG_THROTTLE,
            ExecTailSourceConfigurationConstants.DEFAULT_MSGTYPECONFIG);

    String[] defultTypes = ExecTailSourceConfigurationConstants.DEFAULT_MSGTYPECONFIG_DEFULT.split("\\,");
    for(String oneType : defultTypes){
      String[] oneTypeMap = oneType.split("\\:");
      MsgBuildeJson.MsgTypes.put(oneTypeMap[0],oneTypeMap[1].split("\\|"));
    }

    try {
      if (msgTypeConfig != null && !msgTypeConfig.trim().isEmpty()) {
        String[] userTypes = msgTypeConfig.split("\\,");
        for(String oneType : defultTypes){
          String[] oneTypeMap = oneType.split("\\:");
          if(oneTypeMap.length>=2){
            MsgBuildeJson.MsgTypes.put(oneTypeMap[0],oneTypeMap[1].split("\\|"));
          }
        }
      }
    }catch (Exception ex){
      logger.error("=ExecTailSource.configure=>", ex );
    }

    logger.info("=MsgBuildeJson.MsgTypes is =>"+ JSON.toString(MsgBuildeJson.MsgTypes));


    MsgBuildeJson.MsgIntAtti.addAll(Arrays.asList(ExecTailSourceConfigurationConstants.MAP_INT_ATTRIBUTE.split("\\,")));

    contextIsJson= context.getBoolean(ExecTailSourceConfigurationConstants.CONFIG_CONTEXTISJSON_THROTTLE,
            ExecTailSourceConfigurationConstants.DEFAULT_CONTEXTISJSON);

    contextIsFlumeLog=context.getBoolean(ExecTailSourceConfigurationConstants.CONFIG_CONTEXTISFLUMELOG_THROTTLE,
            ExecTailSourceConfigurationConstants.DEFAULT_CONTEXTISFLUMELOG);

    domain=context.getString(ExecTailSourceConfigurationConstants.CONFIG_DOMIAN_THROTTLE,
            ExecTailSourceConfigurationConstants.DEFAULT_DOMAIN);

    fileWriteJson= context.getString(ExecTailSourceConfigurationConstants.CONFIG_FILEWRITEJSON_THROTTLE,
            ExecTailSourceConfigurationConstants.DEFAULT_FILEWRITEJSON);

    flushTime= context.getLong(ExecTailSourceConfigurationConstants.CONFIG_FLUSHTIME_THROTTLE,
            ExecTailSourceConfigurationConstants.DEFAULT_FLUSHTIME);

    restartThrottle = context.getLong(ExecTailSourceConfigurationConstants.CONFIG_RESTART_THROTTLE,
            ExecTailSourceConfigurationConstants.DEFAULT_RESTART_THROTTLE);

    tailing = context.getString(ExecTailSourceConfigurationConstants.CONFIG_TAILING_THROTTLE,
            ExecTailSourceConfigurationConstants.DEFAULT_ISTAILING_TRUE);

    readinterval=context.getInteger(ExecTailSourceConfigurationConstants.CONFIG_READINTERVAL_THROTTLE,
            ExecTailSourceConfigurationConstants.DEFAULT_READINTERVAL);

    startAtBeginning=context.getBoolean(ExecTailSourceConfigurationConstants.CONFIG_STARTATBEGINNING_THROTTLE,
            ExecTailSourceConfigurationConstants.DEFAULT_STARTATBEGINNING);

    restart = context.getBoolean(ExecTailSourceConfigurationConstants.CONFIG_RESTART,
            ExecTailSourceConfigurationConstants.DEFAULT_RESTART_TRUE);

    logStderr = context.getBoolean(ExecTailSourceConfigurationConstants.CONFIG_LOG_STDERR,
            ExecTailSourceConfigurationConstants.DEFAULT_LOG_STDERR);

    bufferCount = context.getInteger(ExecTailSourceConfigurationConstants.CONFIG_BATCH_SIZE,
            ExecTailSourceConfigurationConstants.DEFAULT_BATCH_SIZE);

    batchTimeout = context.getLong(ExecTailSourceConfigurationConstants.CONFIG_BATCH_TIME_OUT,
            ExecTailSourceConfigurationConstants.DEFAULT_BATCH_TIME_OUT);

    charset = Charset.forName(context.getString(ExecTailSourceConfigurationConstants.CHARSET,
            ExecTailSourceConfigurationConstants.DEFAULT_CHARSET));


    if (sourceCounter == null) {
      sourceCounter = new SourceCounter(getName());
    }
  }

  /**
   * 获取指定路径下的所有文件列表
   *
   * @return
   */
  public  List<String> getFileList() {
    List<String> fatherFilesList = new ArrayList<String>();
    List<String> listFile = new ArrayList<String>();
    String[] baseFatherFiles =basefilepath.split("\\|");
    for(String  oneBaseFile : baseFatherFiles){
      fatherFilesList.addAll(getPassDomainDir(oneBaseFile));
    }
    logger.info("=getFileList=> fatherfiles is "+JSON.toString(fatherFilesList));
    if(fatherFilesList != null || fatherFilesList.size()>0){
      for(String oneDir :fatherFilesList ){
        listFile.addAll(getChildrenFileList(oneDir));
      }
    }
    return listFile;
  }

  public List<String> getPassDomainDir(String dir){
    String[] passFileNames = filepath.split("\\|");
    List<String> listReturn  =  new ArrayList<String>();
    ArrayList<String> PassFileListName = new ArrayList<String>(Arrays.asList(passFileNames));
    File dirFile = new File(dir);
    File[] files = dirFile.listFiles();
    for(File oneCheckfile :files ){
      if(PassFileListName.contains("all")){
        listReturn.addAll(getFilesByConfig(oneCheckfile.getAbsolutePath()));
      } else if(PassFileListName.contains(getDomainName(oneCheckfile.getAbsolutePath()))){
        listReturn.addAll(getFilesByConfig(oneCheckfile.getAbsolutePath()));
      }
    }
    return listReturn;
  }

  private List<String>  getFilesByConfig(String dir){
    List<String> listReturn  =  new ArrayList<String>();
    String apiFile =  getPassApiDir(dir);
    String operateFile =  getPassOperateDir(dir);
    String tomcatFile =  getPassTomcatDir(dir);


    if(apiFile != null && !apiFile.isEmpty()){
      listReturn.add(apiFile);
    }
    if(operateFile != null && !operateFile.isEmpty()){
      listReturn.add(operateFile);
    }
    if(tomcatFile != null && !tomcatFile.isEmpty()){
      listReturn.add(tomcatFile);
    }
    return listReturn;
  }

  public String getPassApiDir(String dir){
    if(apilogsList.contains("all")){
      dir+="/apilogs";
    }else if(apilogsList.contains( getDomainName(dir))){
      dir+="/apilogs";
    }else{
      return null;
    }

    return dir;
  }

  public String getPassOperateDir(String dir){
    if(operatelogsList.contains("all")){
      dir+="/operatelogs";
    }else if(operatelogsList.contains( getDomainName(dir))){
      dir+="/operatelogs";
    }else{
      return null;
    }
    return dir;
  }

  public String getPassTomcatDir(String dir){
    if(tomcatlogsList.contains("all")){
      dir+="/tomcatlogs";
    }else if(tomcatlogsList.contains( getDomainName(dir))){
      dir+="/tomcatlogs";
    }else{
      return null;
    }
    return dir;
  }

  private static String getDomainName(String filePath){
    String[] strs = filePath.split("/");
    String domain ;
    domain=strs[strs.length-1];
    if(domain==null || domain.isEmpty()){
      domain=filePath;
    }
    return domain;
  }

  public  List<String> getChildrenFileList(String dir){

      List<String> listFile = new ArrayList<String>();
      File dirFile = new File(dir);
      //如果不是目录文件，则直接返回
      if (dirFile.isDirectory()) {
        //获得文件夹下的文件列表，然后根据文件类型分别处理
        File[] files = dirFile.listFiles();
        if (null != files && files.length > 0) {
          //根据时间排序
          Arrays.sort(files, new Comparator<File>() {
            public int compare(File f1, File f2) {
              return (int) (f1.lastModified() - f2.lastModified());
            }

            public boolean equals(Object obj) {
              return true;
            }
          });
          for (File file : files) {
            //如果不是目录，直接添加
            if (!file.isDirectory()) {
              String oneFileName = file.getName();
              if(match(filenameRegExp,oneFileName)){
                listFile.add(file.getAbsolutePath());
                logger.info("filename:{} is pass",oneFileName);
              }
            } else {
              //对于目录文件，递归调用
              listFile.addAll(getChildrenFileList(file.getAbsolutePath()));
            }
          }
        }
      }else{
        logger.info("FilePath:{} is not Directory",dir);
      }
      return listFile;

  }



  /**
   * @param regex
   * 正则表达式字符串
   * @param str
   * 要匹配的字符串
   * @return 如果str 符合 regex的正则表达式格式,返回true, 否则返回 false;
   */
  private boolean match(String regex, String str) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(str);
    return matcher.find();
  }


  private class ExecTailRunnable implements Runnable {

    public ExecTailRunnable(
                        SourceCounter sourceCounter, boolean restart, long restartThrottle,
                        boolean logStderr, int bufferCount, long batchTimeout,
                        Charset charset, String filepath,
                        String tailing, Integer readinterval,
                        boolean startAtBeginning, boolean contextIsJson,
                        Properties prop, String fileWriteJson, Long flushTime,
                        boolean contextIsFlumeLog, String domain) {

      this.sourceCounter = sourceCounter;
      this.restartThrottle = restartThrottle;
      this.bufferCount = bufferCount;
      this.batchTimeout = batchTimeout;
      this.restart = restart;
      this.logStderr = logStderr;
      this.charset = charset;
      this.filepath=filepath;
      this.logfile=new File(filepath);
      this.tailing=tailing;
      if(tailing.equals("all")){
        this.isTailing=true;
      }else{
        this.isTailing = Arrays.asList( tailing.split("\\|")).contains(getDomainName(filepath));
      }
      this.readinterval=readinterval;
      this.startAtBeginning=startAtBeginning;
      this.contextIsJson=contextIsJson;
      this.prop = prop;
      this.fileWriteJson=fileWriteJson;
      this.flushTime=flushTime;
      this.contextIsFlumeLog=contextIsFlumeLog;
      this.domain=getDomain(filepath);
    }




    private final SourceCounter sourceCounter;
    private volatile boolean restart;
    private final long restartThrottle;
    private final int bufferCount;
    private long batchTimeout;
    private final boolean logStderr;
    private final Charset charset;
    private SystemClock systemClock = new SystemClock();
    private Long lastPushToChannel = systemClock.currentTimeMillis();
    ScheduledExecutorService timedFlushService;
    ScheduledFuture<?> future;
    private String filepath;
    private boolean contextIsJson;
    private Properties prop;
    private long timepoint;
    private String fileWriteJson;
    private Long flushTime;
    private String domain;

    /**
     * 当读到文件结尾后暂停的时间间隔
     */
    private long readinterval = 10000;

    /**
     * 设置日志文件
     */
    private File logfile;

    /**
     * 设置是否从头开始读
     */
    private boolean startAtBeginning = false;

    /**
     * 设置tail运行标记
     */
    private String tailing = "all";
    private boolean  isTailing = false;

    private boolean contextIsFlumeLog=false;

    private  String getDomain(String filePath){
      String[] strs = filePath.split("/");
      String domain ;
      domain=strs[strs.length-3];
      if(domain==null || domain.isEmpty()){
        domain=filePath;
      }
      return domain;
    }

    @Override
    public void run() {
      logger.info("=run=>main begin");
      Transaction tt = null;
      try {
        do {
          logger.info("=run=>main beginReadLog");
          beginReadLog();
        } while (restart);
      }catch (Exception ex){
        logger.error("=run=> main:",ex);
        try{
          tt=  Profiler.registerInfo("service", "org.apache.flume.source.ExecTailSource.ExecTailRunnable.run");
          Profiler.functionError(tt, ex);
        }catch (Exception exp){
          logger.error("=run=>tt error ",exp);
        }
      }finally {
        if(tt!=null){
          logger.info("=run=>main Profiler.registerEnd(tt);");
          Profiler.registerEnd(tt);
        }
      }
      logger.info("=run=>main end");
    }

    private void beginReadLog(){
        logger.info("=beginReadLog=> flume tail source run start time:"+new Date().toString());
        timepoint=System.currentTimeMillis();
        Long filePointer = null;
        if (this.startAtBeginning) { //判断是否从头开始读文件
          filePointer =0L;
        } else {
          if(prop!=null || prop.contains(filepath)){

            try {
              filePointer = Long.valueOf((String) prop.get(filepath));
              logger.info("=ExecTailRunnable.beginReadLog=>filePointer get from  Properties");
            }catch (Exception ex){
              logger.error("=ExecTailRunnable.beginReadLog=>",ex);
              logger.info("=ExecTailRunnable.beginReadLog=> error filePointer get from file size");
              filePointer=null;
            }
          }
          if(filePointer ==null){
            filePointer = this.logfile.length(); //指针标识从文件的当前长度开始。
            logger.info("=ExecTailRunnable.beginReadLog=>filePointer get from file size");
          }

        }

        final List<Event> eventList = new ArrayList<Event>();

        timedFlushService = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat(
                        "timedFlushExecService" +
                                Thread.currentThread().getId() + "-%d").build());
        RandomAccessFile randomAccessFile = null;
        try {

          logger.info("=ExecTailRunnable.beginReadLog=> begin create RandomAccessFile for "+logfile);
          randomAccessFile= new RandomAccessFile(logfile, "r"); //创建随机读写文件
          future = timedFlushService.scheduleWithFixedDelay(new Runnable() {
                                                              @Override
                                                              public void run() {
                                                                try {
                                                                  synchronized (eventList) {
                                                                    if(!eventList.isEmpty() && timeout()) {
                                                                      flushEventBatch(eventList);
                                                                    }
                                                                  }
                                                                } catch (Exception e) {
                                                                  logger.error("=ExecTailRunnable.beginReadLog=>scheduleWithFixedDelay Exception occured when processing event batch", e);
                                                                  if(e instanceof InterruptedException) {
                                                                    Thread.currentThread().interrupt();
                                                                  }
                                                                }
                                                              }
                                                            },
                  batchTimeout, batchTimeout, TimeUnit.MILLISECONDS);
          logger.info("=ExecTailRunnable.beginReadLog=> begin while isTailing:"+isTailing);
          while (this.isTailing) {
            logger.info(this.filepath+"=ExecTailRunnable.beginReadLog=> while begin isTailing:"+isTailing);
            long fileLength = this.logfile.length();
            if (fileLength < filePointer) {
              logger.info(this.filepath+"=ExecTailRunnable.beginReadLog=> fileLength < filePointer create RandomAccessFile for "+logfile);
              randomAccessFile = new RandomAccessFile(logfile, "r");
              filePointer = 0l;
            }
            if (fileLength > filePointer) {
              randomAccessFile.seek(filePointer);
              String line = randomAccessFile.readLine();
              logger.info(this.filepath+"=1=>randomAccessFile.readLine:"+line);

              //读到空行
              if(line==null||line.trim().length()<1){
                int io_c = 0;
                while (true){
                  io_c=io_c+1;
                  logger.info(this.filepath+"=randomAccessFile.readLine=> line is null");
                  filePointer = randomAccessFile.getFilePointer();
                  line=randomAccessFile.readLine();
                  if ((line!=null && line.trim().length()>0) || fileLength <= filePointer) {
                    break;
                  }
                  if(io_c%10==0){
                    try {
                      Thread.sleep(10000);
                    }catch (Exception ex){
                      logger.warn("Thread.sleep(10000)",ex);
                    }
                  }
                }
              }

              if(line!=null&&line.trim().length()>0){
                line = new String(line.getBytes(ExecTailSourceConfigurationConstants.CHARSET_RANDOMACCESSFILE),charset);
                line = line.replaceAll("\"","\'");
              }

              while (line != null && line.trim().length()>0) {
                logger.info(this.filepath+"=beginReadLog=>do while begin:");
                //送channal
                synchronized (eventList) {
                  sourceCounter.incrementEventReceivedCount();
                  String bodyjson = "";
                  if (!contextIsJson) {
                    bodyjson = MsgBuildeJson.buildeJson(contextIsFlumeLog,line,filepath,domain);
                    if(bodyjson.indexOf("{")>0){
                      bodyjson = bodyjson.substring(bodyjson.indexOf("{"),bodyjson.length());
                    }
                  }else{
                    bodyjson = MsgBuildeJson.changeDomain(line.toString(),domain);
                  }

                  Event oneEvent = EventBuilder.withBody(bodyjson.getBytes(charset));
                  oneEvent.getHeaders().put("UlogDomain",domain);
                  oneEvent.getHeaders().put("topic","file_channel_topic_"+domain);
                  eventList.add(oneEvent);
                  logger.info(this.filepath+"=eventList.add(oneEvent)=>eventList.size:"+eventList.size());
                  if (eventList.size() >= bufferCount || timeout()) {
                    flushEventBatch(eventList);
                    eventList.clear();
                  }
                }

                //读下一行
                line = randomAccessFile.readLine();
                logger.info(this.filepath+"=2=>randomAccessFile.readLine:"+line);
                if(line!=null&&line.trim().length()>0){
                  line = new String(line.getBytes(ExecTailSourceConfigurationConstants.CHARSET_RANDOMACCESSFILE),charset);
                  line = line.replaceAll("\"","\'");
                }else{
                  fileLength=this.logfile.length();
                  int io_c = 0;
                  while (true){
                    io_c=io_c+1;
                    logger.info(this.filepath+"=2=>randomAccessFile.readLine=> line is null");
                    filePointer = randomAccessFile.getFilePointer();
                    line=randomAccessFile.readLine();
                    if ((line!=null && line.trim().length()>0) || fileLength <= filePointer) {
                      break;
                    }
                    if(io_c%10==0){
                      try {
                        Thread.sleep(10000);
                      }catch (Exception ex){
                        logger.warn("Thread.sleep(10000)",ex);
                      }
                    }
                  }
                }
                try{
                  Cat.logMetricForCount("count_file_readline_"+domain);
                }catch (Exception ex){
                  logger.error(this.filepath+"=run=>count_file_readline_"+domain,ex);
                }
                try {
                  Long nowfilePointer = randomAccessFile.getFilePointer();
                  if (!nowfilePointer.equals(filePointer)) {
                    filePointer = nowfilePointer;
                    if (System.currentTimeMillis() - timepoint > flushTime) {
                      timepoint = System.currentTimeMillis();
                      prop.setProperty(filepath, filePointer.toString());
                      FileOutputStream fos = new FileOutputStream(fileWriteJson);
                      if (fos != null) {
                        prop.store(fos, "Update '" + filepath + "' value");
                      }
                      fos.close();

                    }
                  }
                }catch(Exception ex){
                  logger.error(this.filepath+"=ExecTailSource.beginReadLog=> error:",ex);
                }
              }
              logger.info(this.filepath+"=ExecTailSource.beginReadLog=> line is null");
            }else{
              logger.info(this.filepath+"=ExecTailSource.beginReadLog=>fileLength <= filePointer");
            }
            Thread.sleep(this.readinterval);
          }
          logger.info(this.filepath+"=ExecTailRunnable.beginReadLog=>end while  end isTailing:"+isTailing);
          synchronized (eventList) {
            if(!eventList.isEmpty()) {
              flushEventBatch(eventList);
            }
          }

        } catch (Exception e) {
          logger.error("Failed while running filpath: " + filepath, e);
          if(e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
          }
        } finally {

          if(randomAccessFile!=null){
            try {
              randomAccessFile.close();
            } catch (IOException ex) {
              logger.error("Failed to close reader for ExecTail source", ex);
            }
          }

        }
      logger.info(this.filepath+"=run=> flume tail source run restart:"+restart);
        if(restart) {
          logger.info(this.filepath+"=run=> flume tail source run restart time:"+new Date().toString());
          logger.info(this.filepath+"Restarting in {}ms", restartThrottle);
          try {
            Thread.sleep(restartThrottle);
          } catch (InterruptedException e) {
            logger.error("=run=>",e);
            Thread.currentThread().interrupt();
          }
        } else {
          logger.info(this.filepath+"filepath [" + filepath + "] exited with restart[" + restart+"]");
        }

    }

    private void flushEventBatch(List<Event> eventList){
      logger.info(this.filepath+"=ExecTailRunnable.flushEventBatch=> begin eventListsize:"+eventList.size());
      try {
        if(eventList==null||eventList.size()<=0){
          logger.info(this.filepath+"=ExecTailRunnable.flushEventBatch=>eventList==null||eventList.size()<=0 return");
          return;
        }
        getChannelProcessor().processEventBatch(eventList);
        logger.info(this.filepath+"=ExecTailRunnable.flushEventBatch=>sourceCounter.addToEventAcceptedCount size:" + eventList.size());
        sourceCounter.addToEventAcceptedCount(eventList.size());
        eventList.clear();
        lastPushToChannel = systemClock.currentTimeMillis();
        logger.info(this.filepath+"=ExecTailRunnable.flushEventBatch=>addToEventAcceptedCount end");
        try {
          Cat.logMetricForCount("count_file_flushEventBatch_" + domain);
        } catch (Exception ex) {
          logger.error(this.filepath+"=run=>cat error count_file_flushEventBatch_" + domain, ex);
        }
      }catch (Exception exe){
        logger.error(this.filepath+"=ExecTailRunnable.flushEventBatch=>error ",exe);
      }
      logger.info(this.filepath+"=ExecTailRunnable.flushEventBatch=> end");
    }

    private HashMap ParseFlumeLog(String log,HashMap logMap){
      String[] strLogs = log.split("\\|");
      logMap.put("className",strLogs[0]);
      logMap.put("methodName",strLogs[1]);
      logMap.put("level",strLogs[2]);
      logMap.put("treeId",strLogs[3]);
      logMap.put("requestId",strLogs[4]);
      logMap.put("transactionId",strLogs[5]);
      return logMap;
    }

    private boolean timeout(){
      return (systemClock.currentTimeMillis() - lastPushToChannel) >= batchTimeout;
    }

    private  String[] formulateShellCommand(String shell, String command) {
      String[] shellArgs = shell.split("\\s+");
      String[] result = new String[shellArgs.length + 1];
      System.arraycopy(shellArgs, 0, result, 0, shellArgs.length);
      result[shellArgs.length] = command;
      return result;
    }

    public int kill() {
      logger.info("=kill=> flume tail source kill start time:"+new Date().toString());
      this.tailing="no";
      synchronized (this.getClass()) {
        try {
          // Stop the Thread that flushes periodically
          if (future != null) {
            future.cancel(true);
          }

          if (timedFlushService != null) {
            timedFlushService.shutdown();
            while (!timedFlushService.isTerminated()) {
              try {
                timedFlushService.awaitTermination(500, TimeUnit.MILLISECONDS);
              } catch (InterruptedException e) {
                logger.debug("Interrupted while waiting for ExecTail executor service "
                        + "to stop. Just exiting.");
                logger.error("=kill=>",e);
                Thread.currentThread().interrupt();
              }
            }
          }
          logger.info("=kill=> flume tail source kill end time:" + new Date().toString());
          return Integer.MIN_VALUE;
        } catch (Exception ex) {
          logger.error("=kill=>", ex);
          Thread.currentThread().interrupt();
        }
      }
      logger.info("=kill=> flume tail source kill end time:"+new Date().toString());
      return Integer.MIN_VALUE / 2;
    }
    public void setRestart(boolean restart) {
      this.restart = restart;
    }
  }
  private static class StderrReader extends Thread {
    private BufferedReader input;
    private boolean logStderr;

    protected StderrReader(BufferedReader input, boolean logStderr) {
      this.input = input;
      this.logStderr = logStderr;
    }



    @Override
    public void run() {
      try {
        int i = 0;
        String line = null;
        while((line = input.readLine()) != null) {
          if(logStderr) {
            // There is no need to read 'line' with a charset
            // as we do not to propagate it.
            // It is in UTF-16 and would be printed in UTF-8 format.
            logger.info("StderrLogger[{}] = '{}'", ++i, line);
          }
        }
      } catch (IOException e) {
        logger.error("StderrLogger exiting", e);
      } catch(Exception ex){
        logger.error("=run=>",ex);
      }finally {
        try {
          if(input != null) {
            input.close();
          }
        } catch (IOException ex) {
          logger.error("Failed to close stderr reader for ExecTail source", ex);
        }catch (Exception exe){
          logger.error("=run=>",exe);
        }
      }
    }
  }
}
