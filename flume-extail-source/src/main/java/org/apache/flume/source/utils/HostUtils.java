package org.apache.flume.source.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * Created by Administrator on 2015/9/18.
 */
public class HostUtils {
    private static String localHostName;
    private static String localHostIp;

    static {
        try {
            localHostName = InetAddress.getLocalHost().getHostName();
            StringBuilder ips = new StringBuilder();
            Enumeration<NetworkInterface> network = NetworkInterface.getNetworkInterfaces();
            while (network.hasMoreElements()) {
                Enumeration<InetAddress> addresses = network.nextElement().getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    if (ip != null && ip instanceof Inet4Address) {
                        if (!"127.0.0.1".equals(ip.getHostAddress())) {
                            ips.append(ip.getHostAddress()).append(",");
                        }
                    }
                }
            }
            if (ips.length() > 0) {
                ips.deleteCharAt(ips.length() - 1);
                localHostIp = ips.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getLocalHostName() {
        if (localHostName == null) {
            localHostName = System.getProperty("machine.name");
        }
        return localHostName;
    }

    public static String getLocalHostIp() {
        return localHostIp;
    }

    public static void main(String args[]) {
        System.out.println("host ip=" + getLocalHostIp());
        System.out.println("host name=" + getLocalHostName());
    }


}
