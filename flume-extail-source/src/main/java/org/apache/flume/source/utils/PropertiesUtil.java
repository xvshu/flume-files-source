package org.apache.flume.source.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by xvshu on 2016/6/8.
 */
public class PropertiesUtil {

    private  Properties prop = new Properties();

    private  String file ="";

    static{



    }

    public PropertiesUtil(String fileStr){
        this.file=fileStr;
        try {
            prop.load(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public  String getProperty(String key){
        return prop.getProperty(key);
    }







    public  void setProper(String key,String value){
        try {
            prop.setProperty(key, value);
            FileOutputStream fos = new FileOutputStream(file);
            prop.store(fos, null);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
