package F5BClient2;

import F5BClient2.Pi_LCD_GPIO.Handler;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.pi4j.wiringpi.SoftTone;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

public class F5BClient2 {
    
    public final static String MENU_SELECTER = "Display Menu";
    public final static String EXIT = "Exit";
    public final static String CLIENT = "Client";
    public final static String NETWORK_CHECK = "Network Check";
    public final static String SCANNER = "Scanner";
    public final static String TIME = "Time";
    public final static String VARIO = "Vario";
    public final static String TESTSOUND = "Sound Test";
    public final static String UPDATE = "Update";
    public final static String CLIENTCHECKER = "Check Clients";
    
    public final static String F5_CLIENT_CONNECTION_URI = "ws://f5.be/ws/client";
        
    Handler lcd_gpio_Handler;
    
    private String selecter;
    
    private static WebSocketContainer container;
    private static F5WebSocketClient f5ClientEndpoint;
    private static SensorPureJavaCommClient sensorClient;
    
    private enum ClientType {F5CLIENT, SENSORCLIENT}
    
    private URI connection;
    private String ip;   
    private String wifi;
    private String qual;
    private int clients;

    public void start(String version) throws Exception {
        lcd_gpio_Handler = new Handler(version);
        lcd_gpio_Handler.writeWelcome();
        
        
        selecter = MENU_SELECTER;
        
        while (!EXIT.equals(selecter)) {
            //System.out.println("LOOPING IN MAIN PROG");
            switch (selecter) {
                case MENU_SELECTER :
                    System.out.println("Displaying menu...");
                    selecter = lcd_gpio_Handler.displayMenu();
                    System.out.println("Menu selection: " + selecter);
                    break;
                case NETWORK_CHECK :
                    setSelecterAndDisplayMenuselection(NETWORK_CHECK);
                    networkCheck();
                    break;
                case CLIENT :
                    setSelecterAndDisplayMenuselection(CLIENT);
                    client(F5_CLIENT_CONNECTION_URI);
                    break;
                case SCANNER :
                    setSelecterAndDisplayMenuselection(SCANNER);
                    scanner();
                    break;
                case TIME :
                    setSelecterAndDisplayMenuselection(TIME);
                    showTime();
                    break;
                case VARIO :
                    setSelecterAndDisplayMenuselection(VARIO);
                    runVario();
                    break;
                case CLIENTCHECKER :
                    setSelecterAndDisplayMenuselection(CLIENTCHECKER);
                    runClientChecker();
                    break;
                case TESTSOUND :
                    setSelecterAndDisplayMenuselection(TESTSOUND);
                    runSoundTest();
                    break;
                case UPDATE :
                    setSelecterAndDisplayMenuselection(UPDATE);
                    if (runUpdate()) {
                        selecter = EXIT;
                    }
                    break;
            }
        }
        
        System.out.println("Cleaning up...");
        lcd_gpio_Handler.writeStop();
        lcd_gpio_Handler.clearScreen();
        
        System.out.println("Exit program and stopping Pi");
        CommandLineFunctions.executeCommand("sudo poweroff");
    }
    
    private void setSelecterAndDisplayMenuselection (String menuSelection) {
        selecter = MENU_SELECTER;
        System.out.println("Running " + menuSelection + "...");
    }
    
    private void showTime() {
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread = new Thread(){
            public void run() {
                try {
                    while (true) {
                        lcd_gpio_Handler.writeLineWithDate(Handler.formatTextToFit1Line("Current Time"));
                    } 
                } catch (Exception e) {
                    System.out.println("Interrupted " + TIME);
                    //e.printStackTrace();
                }
            }
        };
        
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread.start();
        
        try {
            lcd_gpio_Handler.interrupt_Listener.interruptable_Thread.join();
        } catch (InterruptedException e) {
        }
    }
    
    private void runSoundTest() {
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread = new Thread(){
            public void run() {
                try {
                    while (true) {
                        lcd_gpio_Handler.varioTest();
                    } 
                } catch (Exception e) {
                    System.out.println("Interrupted " + TESTSOUND);
                    SoftTone.softToneStop(lcd_gpio_Handler.PIEZO_PIN);
                    //e.printStackTrace();
                }
            }
        };
        
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread.start();
        
        try {
            lcd_gpio_Handler.interrupt_Listener.interruptable_Thread.join();
        } catch (InterruptedException e) {
        }
        
    }
    
    private void scanner() throws InterruptedException, URISyntaxException {
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread = new Thread(){
            public void run() {
                ArrayList<String> singleList;
                try{
                    while (true) {
                        lcd_gpio_Handler.writeLineWithDate(Handler.formatTextToFit1Line("Scanning!..."));
                        ArrayList<ArrayList<String>> listOLists = NetworkFunctions.scan();
                        for (int i = 0; i < listOLists.size(); i++) {
                            singleList = (ArrayList)listOLists.get(i);
                            //System.out.println("Values Qual: " + singleList.get(0));
                            //System.out.println("Values ssid: " + singleList.get(1));
                            lcd_gpio_Handler.write2Lines(i+1 + " of " + listOLists.size() + " => " + singleList.get(0), Handler.formatTextToFit1Line(singleList.get(1)));
                        }
                    }
                    
                } catch (Exception e) {
                    System.out.println("Interrupted " + SCANNER);
                    //e.printStackTrace();
                }
            }
        };
        
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread.start();
        
        try {
            lcd_gpio_Handler.interrupt_Listener.interruptable_Thread.join();
        } catch (InterruptedException e) {
        }
    }
    
    private void networkCheck() throws InterruptedException, URISyntaxException {
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread = new Thread(){
            public void run() {
                try{
                    while (true) {
                        wifi=NetworkFunctions.getSSIDName();
                        lcd_gpio_Handler.writeLineWithDate("SSID:" + wifi);

                        qual=NetworkFunctions.getLinkQuality();
                        lcd_gpio_Handler.writeLineWithDate("Quality:" + qual);
                        
                        ip = NetworkFunctions.getIP();
                        lcd_gpio_Handler.writeLineWithDate("ip:" + ip);
                    }
                } catch (InterruptedException ie) {
                    System.out.println("Interrupted " + NETWORK_CHECK);
                }
            }
        };
        
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread.start();
        
        try {
            lcd_gpio_Handler.interrupt_Listener.interruptable_Thread.join();
        } catch (InterruptedException e) {
        }
    }
    
    private void runVario() throws InterruptedException, URISyntaxException {
        SensorPureJavaCommClient sensorClient = new SensorPureJavaCommClient(lcd_gpio_Handler);
        sensorClient.start();
    }
    
    private boolean runUpdate() {
        try {
            lcd_gpio_Handler.writeLineWithDate("Update Check...");
            boolean fileIsPresent = NetworkFunctions.isUpdateFilePresent();

            if (fileIsPresent) {
                lcd_gpio_Handler.writeLineWithDate("Update found!");
                lcd_gpio_Handler.writeLineWithDate("Updating...");
                NetworkFunctions.copyUpdateFile();
                Thread.sleep(1000);
                lcd_gpio_Handler.writeLineWithDate("Update Done!");
                return true;
                
            } else {
                 lcd_gpio_Handler.writeLineWithDate("No Update...");
                 return false;
            }
        } catch (JSchException | SftpException | IOException | InterruptedException e) {
            System.out.println("Error in runUpdate: " + e.getLocalizedMessage());
            return false;
        }
    }
    
    private void runClientChecker () {
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread = new Thread(){
            public void run() {
                try{
                    while (true) {
                        lcd_gpio_Handler.writeLineWithDate("Running Check...");
                        clients=NetworkFunctions.getNumberOfConnectedClients();
                        lcd_gpio_Handler.writeLineWithDate("# Clients:" + clients);
                        Thread.sleep(2000);
                    }
                } catch (InterruptedException ie) {
                    System.out.println("Interrupted " + CLIENTCHECKER);
                } catch (JSchException ex) {
                    Logger.getLogger(F5BClient2.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(F5BClient2.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
        
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread.start();
        
        try {
            lcd_gpio_Handler.interrupt_Listener.interruptable_Thread.join();
        } catch (InterruptedException e) {
        }                  
    }
    
    private void client(String Connection) throws InterruptedException, URISyntaxException {
        container = ContainerProvider.getWebSocketContainer();
        
        f5ClientEndpoint = new F5WebSocketClient(lcd_gpio_Handler.lcdHandle, lcd_gpio_Handler);
                
        connection = new URI(Connection);
         
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread = new Thread(){
            @Override
            public void run() {
                Integer tries = 0;
                Session session = null;
                Session aliveSession;
                try {
                    while (session == null || !session.isOpen()) {
                        try {
                            wifi = NetworkFunctions.getSSIDName();
                            switch (wifi) {
                                case "off/any":
                                    throw new IOException("No Network");
                                case "f5":
                                    ip = NetworkFunctions.getIP();
                                    lcd_gpio_Handler.writeLineWithDate("ip:" + ip);
                                    //throw new IOException("TEMPLOOP");
                                    break;
                                case "f52":
                                    ip = NetworkFunctions.getIP();
                                    lcd_gpio_Handler.writeLineWithDate("ip:" + ip);
                                    break;
                                default:
                                    throw new IOException("Wrong Network");
                            }
                                    
                            tries++;
                            System.out.println("Connecting try: " + tries.toString() + "...");
                            lcd_gpio_Handler.writeLineWithDate("-- Attempt " + (tries.toString() + " ----").substring(0, 4));
                            System.out.println("Initialising F5Client...");
                            session = container.connectToServer(f5ClientEndpoint, connection);
                            
                            tries = 0;
                            System.out.println("Connected to server");
                            lcd_gpio_Handler.writeLineWithDate("-- Connected! --");
                        } catch (DeploymentException ex) {
                            System.out.println("Unable to connect!");
                            lcd_gpio_Handler.writeLineWithDate("-- No Connect --");
                        } catch (IOException ex) {
                            lcd_gpio_Handler.writeLineWithDate("No-Bad Netw/Sens");
                        }
                        while (session != null && session.isOpen()) {
                            Thread.sleep(10000);
                            try {
                                //try to connect again to see if session is still alive
                                //System.out.println("Connecting to F5Client");
                                aliveSession = container.connectToServer(f5ClientEndpoint, connection);
                                aliveSession.close();
                                //System.out.println("All is well: Still connected!");
                            } catch (DeploymentException ex) {
                                try {
                                    System.out.println("Crap, we can't connect anymore... Killing all sessions.");
                                    session.close();
                                    session = null;
                                    lcd_gpio_Handler.writeLineWithDate("- Disconnected -");
                                } catch (IOException ex1) {
                                    System.out.println("IOException3!");
                                }
                            } catch (IOException ex) {
                                System.out.println("IOException2!");
                            }   
                        }
                    }     
                    try {                
                        session.close();
                    } catch (IOException ex) {
                        System.out.println("IOException4!");
                    }
                } catch (InterruptedException ie) {
                    System.out.println("Interrupted " + CLIENT);
                    if (session != null && session.isOpen()) {
                        try {
                            session.close();
                        } catch (IOException ex) {
                            
                        }
                        session = null;
                    }
                }
            }
        };
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread.start();
        
        try {
            lcd_gpio_Handler.interrupt_Listener.interruptable_Thread.join();
        } catch (InterruptedException e) {
        }
    } 
}
