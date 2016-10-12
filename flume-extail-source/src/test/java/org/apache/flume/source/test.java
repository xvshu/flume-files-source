package org.apache.flume.source;

/**
 * Created by xvshu on 2016/9/29.
 */
public class test
{

    private static String getDomain(String filePath){
        String[] strs = filePath.split("/");
        String domain ;
        domain=strs[strs.length-3];
        if(domain==null || domain.isEmpty()){
            domain=filePath;
        }
        return domain;
    }


    public static void main(String[] args) {
        String filePath = "/Data/soa.el.com/apilogs/solw.log";
        System.out.println(getDomain(filePath));
    }
}
