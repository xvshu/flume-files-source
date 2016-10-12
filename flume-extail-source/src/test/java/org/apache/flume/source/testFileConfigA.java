package org.apache.flume.source;

import org.mortbay.util.ajax.JSON;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Created by xvshu on 2016/10/9.
 */
public class testFileConfigA
{

    private static String  basefilepath = "D:\\export\\home\\tomcat\\logs";
    private static String  filepath = "account.soa.el.net|tender.soa.el.com";

    private static  List<String> apilogsList = new ArrayList<String>();
    private static List<String> operatelogsList = new ArrayList<String>();
    private static List<String> tomcatlogsList =  new ArrayList<String>();
    /**
     * 获取指定路径下的所有文件列表
     *
     * @return
     */
    public static List<String> getFileList() {
        List<String> fatherFilesList = new ArrayList<String>();
        List<String> listFile = new ArrayList<String>();
        String[] baseFatherFiles =basefilepath.split("\\|");
        for(String  oneBaseFile : baseFatherFiles){
            fatherFilesList.addAll(getPassDomainDir(oneBaseFile));
        }
        System.out.println("=getFileList=> fatherfiles is "+ JSON.toString(fatherFilesList));
        if(fatherFilesList != null || fatherFilesList.size()>0){
            for(String oneDir :fatherFilesList ){
                listFile.addAll(getChildrenFileList(oneDir));
            }
        }
        return listFile;
    }

    public static List<String> getPassDomainDir(String dir){
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

    private static List<String>  getFilesByConfig(String dir){
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

    public static String getPassApiDir(String dir){
        if(apilogsList.contains("all")){
            dir+="/apilogs";
        }else if(apilogsList.contains( getDomainName(dir))){
            dir+="/apilogs";
        }else{
            return null;
        }

        return dir;
    }

    public static String getPassOperateDir(String dir){
        if(operatelogsList.contains("all")){
            dir+="/operatelogs";
        }else if(operatelogsList.contains( getDomainName(dir))){
            dir+="/operatelogs";
        }else{
            return null;
        }
        return dir;
    }

    public static String getPassTomcatDir(String dir){
        if(tomcatlogsList.contains("all")){
            dir+="/tomcatlogs";
        }else if(tomcatlogsList.contains( getDomainName(dir))){
            dir+="/tomcatlogs";
        }else{
            return null;
        }
        return dir;
    }

    private  static String getDomainName(String filePath){
        String[] strs =  filePath.split("\\\\");
        String domain ;
        domain=strs[strs.length-1];
        if(domain==null || domain.isEmpty()){
            domain=filePath;
        }
        return domain;
    }

    public static  List<String> getChildrenFileList(String dir){

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
                        listFile.add(file.getAbsolutePath());
                        System.out.println(file.getAbsolutePath());
                    } else {
                        //对于目录文件，递归调用
                        listFile.addAll(getChildrenFileList(file.getAbsolutePath()));
                    }
                }
            }
        }
        return listFile;

    }

    public static void main(String[] args) {
        String apilogs ="account.soa.el.net";
        apilogsList.addAll(Arrays.asList(apilogs.split("\\|")));

        String operatelogs ="no";
        operatelogsList.addAll(Arrays.asList(operatelogs.split("\\|")));

        String tomcatlogs = "no";
        tomcatlogsList.addAll(Arrays.asList(tomcatlogs.split("\\|")));

        getFileList();

    }
}
