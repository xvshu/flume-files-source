package org.apache.flume.source.utils;

import org.apache.flume.source.ExecTailSourceConfigurationConstants;
import org.mortbay.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by xvshu on 2016/7/13.
 */
public class MsgBuildeJson {

    private static final Logger logger = LoggerFactory
            .getLogger(MsgBuildeJson.class);
    public static HashMap<String,String[]> MsgTypes = new HashMap<String, String[]>();
    public static HashSet<String> MsgIntAtti =  new HashSet<String>();
    public static String buildeJson(boolean contextIsFlumeLog ,String context ,String filepath ,String domain ){
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{");
        jsonBuilder.append("\"context\":").append("\"").append(getContext(contextIsFlumeLog,context)).append("\",");
        jsonBuilder.append("\"created\":").append(System.currentTimeMillis()).append(",");
        jsonBuilder.append("\"domain\":").append("\"").append(domain==null||domain.isEmpty()?getDomain(filepath):domain).append("\",");
        jsonBuilder.append("\"localHostIp\":").append("\"").append(HostUtils.getLocalHostIp()).append("\",");
        jsonBuilder.append("\"localHostName\":").append("\"").append(HostUtils.getLocalHostName()).append("\",");
        jsonBuilder.append("\"contextType\":").append("\"").append(filepath).append("\",");
        if(contextIsFlumeLog){
            String[] strLogs = context.split("\\|");
            String[] attiNames =null;
            if(MsgTypes.containsKey(strLogs[0])){
                attiNames = MsgTypes.get(strLogs[0]);
            }else{
                attiNames = MsgTypes.get("defult");
                jsonBuilder.append("\"logType\":").append("\"defult\",");
            }
            int strLenght = strLogs.length > attiNames.length?attiNames.length:strLogs.length;
            for (int i = 0; i <strLenght ; i++) {
                if(MsgIntAtti.contains(attiNames[i])){
                    jsonBuilder.append("\"").append(attiNames[i]).append("\":").append(strLogs[i]).append(",");
                }else{
                    jsonBuilder.append("\"").append(attiNames[i]).append("\":").append("\"").append(strLogs[i]).append("\",");
                }

            }

        }
        jsonBuilder.append("\"filepath\":").append("\"").append(filepath).append("\"");
        jsonBuilder.append("}");
        return jsonBuilder.toString();

    }

    private static String getContext(boolean contextIsFlumeLog,String line){
        try {
            StringBuilder stringBuilder = new StringBuilder();
            if (contextIsFlumeLog) {

                String[] strLogs = line.split("\\|");
                int attiCount = 0;
                if (MsgTypes.containsKey(strLogs[0])) {
                    attiCount = MsgTypes.get(strLogs[0]).length;
                } else {
                    attiCount = MsgTypes.get("defult").length;
                }

                if (attiCount >= strLogs.length) {
                    return line;
                }
                for (int i = attiCount; i < strLogs.length; i++) {
                    stringBuilder.append(strLogs[i]);
                }

                return stringBuilder.toString();


            } else {
                return line;
            }
        }catch(Exception ex){
            ex.printStackTrace();
            return line;
        }

    }

    private static String getDomain(String filePath){
        String[] strs = filePath.split("/");
        String domain ;
        domain=strs[strs.length-2];
        if(domain==null || domain.isEmpty()){
            domain=filePath;
        }
        return domain;
    }

    public static String changeDomain (String context,String domain){
        if(domain==null || domain.isEmpty()){
            return context;
        }
        if(context.indexOf("{")>0){
            context= context.substring(context.indexOf("{"),context.length());
        }
        if(context.indexOf("domain")>0){
            HashMap map = (HashMap)JSON.parse(context);
            map.put("domain",domain);
            context=JSON.toString(map);
        }else{
            context = context.substring(0,context.length()-1);
            context = context +",\"domain\":\""+domain+"\"}";
        }

        return context;

    }



    public static void main(String[] args ){

        String[] defultTypes = ExecTailSourceConfigurationConstants.DEFAULT_MSGTYPECONFIG_DEFULT.split("\\,");
        for(String oneType : defultTypes){
            String[] oneTypeMap = oneType.split("\\:");
            MsgBuildeJson.MsgTypes.put(oneTypeMap[0],oneTypeMap[1].split("\\|"));
        }
        System.out.println(getContext(true,"com.el.common.log.LogAop|logMsg|ERROR|13632462968687849|13632462968687849|13632463237123305|DubboServerHandler-192.168.10.82:20882-thread-200 2016-08-10 19:02:24,861 [5626366] - exec com.el.account.soa.service.account.fundsinfo.FundsInfoServiceImpl.getOutIncomeByTid() spend : 841 milliseconds."));
        System.out.println(getContext(true,"PerformanceFile|com.el.account.soa.service.logic.fundsinfo.LogicFundsInfoServiceImpl|addOutgo|INFO|13632526051020009|13632526051020009|13632526051020009|yes|1470827297188|1470827297197|9|no config|no config|2016-08-10 19:08:17,197 - <=addOutgo=>performsfile log is end "));
        System.out.println(getContext(true,"12345678"));

    }

}
