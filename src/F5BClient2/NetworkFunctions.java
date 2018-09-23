package F5BClient2;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;

public class NetworkFunctions {

    public static String getSSIDName() {
        String command = "sudo iwconfig";
        
        String temp;
        temp = CommandLineFunctions.executeCommand(command);
        String ssid = getValuesForStringFromText(temp, "ESSID:", "\n").get(0);
        ssid = ssid.trim();
        if (ssid.length() != 0 && ssid.startsWith("\"")) {
            ssid = ssid.substring(1, ssid.length()-1);
        }
        return ssid;
        
    }
    
    public static String getLinkQuality() {
        String command = "sudo iwconfig";
        
        String temp;
        temp = CommandLineFunctions.executeCommand(command);
        LinkedList<String> linkQuality = getValuesForStringFromText(temp, "Link Quality=", "  ");
        if (linkQuality.size() > 0) {
            return linkQuality.get(0).trim();
        }  else {
            return "";
        }
        
    }
    
    public static ArrayList scan() {
        String command = "sudo iwlist wlan0 scan";
        
        String temp;
        System.out.println("Scanning...");
        temp = CommandLineFunctions.executeCommand(command);
        System.out.println("Scan complete");
        String valueFrom;
        Integer valueTo;
        LinkedList<String> essids = new LinkedList<>();
        LinkedList<String> qualities = new LinkedList<>();
        ArrayList<ArrayList<String>> listOLists = new ArrayList<ArrayList<String>>();
        
        essids = getValuesForStringFromText(temp, "ESSID:\"", "\"");
               
        qualities = getValuesForStringFromText(temp, "Quality=", "  ");
        
        if (qualities.size() == essids.size()) {
            for (int i = 0; i < qualities.size(); i++) {
                ArrayList<String> singleList = new ArrayList<String>();
                singleList.add(qualities.get(i));
                singleList.add(essids.get(i));
                listOLists.add(singleList);
            }
        }
        return listOLists;
    }
    
    private static LinkedList<String> getValuesForStringFromText(String textToSearch, String textToLookFor, String endTextToLookFor) {
        String valueFrom;
        Integer valueTo;
        LinkedList<String> returnValue = new LinkedList<>();
        for (int index = textToSearch.indexOf(textToLookFor); index >= 0; index = textToSearch.indexOf(textToLookFor, index + 1)) { 
             if (index != -1) {    
                valueFrom = textToSearch.substring(index+textToLookFor.length());
                valueTo = valueFrom.indexOf(endTextToLookFor);
                if (valueTo != -1) {
                    returnValue.add(valueFrom.substring(0, valueTo));
                }
            }
        }
        //System.out.println(returnValue);
        return returnValue;
    }
    
    public static String getIP() {
        String local_ip = "";
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual() || iface.isPointToPoint())
                    continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    if(Inet4Address.class == addr.getClass()) {
                        local_ip = addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return local_ip;
    }
       
}
