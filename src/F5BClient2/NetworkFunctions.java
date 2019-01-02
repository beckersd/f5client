package F5BClient2;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;

public class NetworkFunctions {
    
    private static String host="1.1.1.2";
    private static String user="pi";
    private static String password="myPass1";
    private static String lookForFileCommand = "ls /home/pi/NetBeansProjects/f5client2/dist/";
    private static String remoteFolderName = "/home/pi/NetBeansProjects/f5client2/dist/";
    private static String remoteFileName = "F5BClient2.jar";
    private static String localFolderName = "/home/pi/NetBeansProjects/f5client2/dist/";
    private static String localFileName = "F5BClient2.jar";
    private static String numberOfClientsCommand = "sudo iw dev wlan0 station dump";
    private static String textToIdentifyConnectedClients = "Station";
    
    private static Session connectToPi() throws JSchException {
        java.util.Properties config = new java.util.Properties(); 
        config.put("StrictHostKeyChecking", "no");
        System.out.println("SSHing into Pi...");
        JSch jsch = new JSch();
        Session session=jsch.getSession(user, host, 22);
        session.setPassword(password);
        session.setConfig(config);
        session.connect(5000);
        System.out.println("Connected");
        return session;
    }
    
    public static boolean isUpdateFilePresent() throws JSchException, IOException {
        Session session = connectToPi();
        
        System.out.println("Executing Look For File Command...");
        Channel channel=session.openChannel("exec");
         ((ChannelExec)channel).setCommand(lookForFileCommand + remoteFileName);
        channel.setInputStream(null);
        ((ChannelExec)channel).setErrStream(System.err);

        InputStream in=channel.getInputStream();
        channel.connect();
        byte[] tmp=new byte[1024];
        String lsreturn = "";
        while(true){
          while(in.available()>0){
            int i=in.read(tmp, 0, 1024);
            if(i<0)break;
            lsreturn = lsreturn + new String(tmp, 0, i);
            lsreturn = lsreturn.trim();
            //System.out.print("$" + lsreturn + "$");
          }
          if(channel.isClosed()){
            System.out.println("exit-status: "+channel.getExitStatus());
            break;
          }
          try{Thread.sleep(1000);}catch(Exception ee){}
        }

        System.out.println("Executing Command: DONE");
        session.disconnect();

        if (lsreturn.contains(remoteFileName)) {
            System.out.println("File found!");
            return true;
        }
        
        return false;
    }
    
    public static void downloadUpdateFile() throws JSchException, SftpException {
        Session session = connectToPi();
        
        System.out.println("Downloading file...");
        Channel channel = session.openChannel("sftp");
        channel.connect();
        ChannelSftp sftpChannel = (ChannelSftp) channel;

        sftpChannel.get(remoteFolderName + remoteFileName, localFolderName + localFileName);
        sftpChannel.exit();  
        channel.disconnect();

        System.out.println("Copying file: DONE");

        session.disconnect();
        
    }
    
    public static void uploadUpdateFile() throws JSchException, SftpException, FileNotFoundException {
        //TODO initial test for this function... still to integrate
        Session session = connectToPi();
          
        System.out.println("Uploading file...");
        Channel channel = session.openChannel("sftp");
        channel.connect();
        ChannelSftp sftpChannel = (ChannelSftp) channel;
        
        //
        // Change to the remote directory
        //
        System.out.println("Changing to FTP remote dir: " + remoteFolderName);
        sftpChannel.cd(remoteFolderName);

        //
        // Send the file we generated
        //
        File f = new File(localFolderName + localFileName);
        System.out.println("Storing file as remote filename: " + f.getName());
        sftpChannel.put(new FileInputStream(f), f.getName());

        sftpChannel.exit();
        channel.disconnect();
        
        session.disconnect();
        
    }
    
    public static int getNumberOfConnectedClients() throws JSchException, IOException {
        Session session = null;
        try {
            session = connectToPi();
        } catch (JSchException j) { 
        }
        if (session == null) {
            return 0;
        }
        
        System.out.println("Executing Client Check Command...");
        Channel channel=session.openChannel("exec");
        ((ChannelExec)channel).setCommand(numberOfClientsCommand);
        channel.setInputStream(null);
        ((ChannelExec)channel).setErrStream(System.err);

        InputStream in=channel.getInputStream();
        channel.connect();
        byte[] tmp=new byte[1024];
        String lsreturn = "";
        while(true){
          while(in.available()>0){
            int i=in.read(tmp, 0, 1024);
            if(i<0)break;
            lsreturn = lsreturn + new String(tmp, 0, i);
            lsreturn = lsreturn.trim();
            //System.out.print("$" + lsreturn + "$");
          }
          if(channel.isClosed()){
            System.out.println("exit-status: "+channel.getExitStatus());
            break;
          }
          try{Thread.sleep(1000);}catch(Exception ee){}
        }

        channel.disconnect();
        session.disconnect();
        System.out.println("Client Check Command DONE");

        int count = lsreturn.split(textToIdentifyConnectedClients, -1).length-1;
        //System.out.println("Number of Clients connected: " + count);
        
        return count;
    }

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
