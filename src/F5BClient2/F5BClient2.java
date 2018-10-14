package F5BClient2;

import F5BClient2.Pi_LCD_GPIO.Handler;
import com.pi4j.wiringpi.SoftTone;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
    
    public final static String F5_CLIENT_CONNECTION_URI = "ws://f5.be/ws/client";
    public final static String SENSOR_CONNECTION_URI = "ws://localhost:1880/ws/sensor";
        
    Handler lcd_gpio_Handler;
    
    private String selecter;
    
    private static WebSocketContainer container;
    private static F5WebSocketClient f5ClientEndpoint;
    private static SensorWebSocketClient sensorClientEndpoint;
    
    private enum ClientType {F5CLIENT, SENSORCLIENT}
    
    private URI connection;
    private String ip;   
    private String wifi;
    private String qual;

    public void start() throws Exception {
        lcd_gpio_Handler = new Handler();
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
                    selecter = MENU_SELECTER;
                    System.out.println("Running " + NETWORK_CHECK + "...");
                    networkCheck();
                    break;
                case CLIENT :
                    selecter = MENU_SELECTER;
                    System.out.println("Running " + CLIENT + "...");
                    client(F5_CLIENT_CONNECTION_URI, ClientType.F5CLIENT);
                    break;
                case SCANNER :
                    selecter = MENU_SELECTER;
                    System.out.println("Running " + SCANNER + "...");
                    scanner();
                    break;
                case TIME :
                    selecter = MENU_SELECTER;
                    System.out.println("Running " + TIME + "...");
                    showTime();
                    break;
                case VARIO :
                    selecter = MENU_SELECTER;
                    System.out.println("Running " + VARIO + "...");
                    //runVario();
                    client(SENSOR_CONNECTION_URI, ClientType.SENSORCLIENT);
                    break;  
            }
        }
        
        System.out.println("Stopping...");
        lcd_gpio_Handler.writeStop();
        lcd_gpio_Handler.clearScreen();
        
        System.out.println("Exit program and stopping Pi");
        CommandLineFunctions.executeCommand("sudo poweroff");
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
    
    private void runVario() {
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread = new Thread(){
            public void run() {
                try {
                    while (true) {
                        lcd_gpio_Handler.varioTest();
                        client(SENSOR_CONNECTION_URI, ClientType.SENSORCLIENT);
                    } 
                } catch (Exception e) {
                    System.out.println("Interrupted " + VARIO);
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
    
    private void client(String Connection, ClientType clientType) throws InterruptedException, URISyntaxException {
        container = ContainerProvider.getWebSocketContainer();
        switch (clientType) {
            case F5CLIENT:
                //System.out.println("Initialising F5Client...");
                f5ClientEndpoint = new F5WebSocketClient(lcd_gpio_Handler.lcdHandle);
                break;
                
            case SENSORCLIENT:
                //System.out.println("Initialising Sensorclient...");
                //lcd_gpio_Handler.varioTest();
                sensorClientEndpoint = new SensorWebSocketClient(lcd_gpio_Handler);
                break;
        }
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
                            switch (clientType) {
                                case F5CLIENT:
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
                                    break;
                            }

                            tries++;
                            System.out.println("Connecting try: " + tries.toString() + "...");
                            lcd_gpio_Handler.writeLineWithDate("-- Attempt " + (tries.toString() + " ----").substring(0, 4));
                            switch (clientType) {
                                case F5CLIENT:
                                System.out.println("Initialising F5Client...");
                                session = container.connectToServer(f5ClientEndpoint, connection);
                                break;
                
                            case SENSORCLIENT:
                                System.out.println("Initialising Sensorclient: " + connection + "...");
                                session = container.connectToServer(sensorClientEndpoint, connection);
                                sensorClientEndpoint.initialConnectOk = true;
                                break;
                            }
                            
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
                                switch (clientType) {
                                    case F5CLIENT:
                                        //System.out.println("Connecting to F5Client");
                                        aliveSession = container.connectToServer(f5ClientEndpoint, connection);
                                        aliveSession.close();
                                        break;
                                    case SENSORCLIENT:
                                        //System.out.println("Connecting to SensorClient");
                                        aliveSession = container.connectToServer(sensorClientEndpoint, connection);
                                        sensorClientEndpoint.initialConnectOk = true;
                                        aliveSession.close();
                                        break;
                                }    
                                
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
