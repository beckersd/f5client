package F5BClient2.Pi_LCD_GPIO;

import F5BClient2.F5BClient2;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.Lcd;
import com.pi4j.wiringpi.SoftTone;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Handler {
    
    private final static int LCD_ROWS = 2;
    private final static int LCD_COLUMNS = 16;
    private final static int LCD_BITS = 4;
    
    private final static int GPIO_PIN = 26;
    
    public final static int PIEZO_PIN = 6;

    public int lcdHandle;
    private final SimpleDateFormat formatter;
    
    public Interrupt_Listener interrupt_Listener;
    private String selectedOption;
    
    public Handler() throws InterruptedException {
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                int lcdHandle= getLcdHandle();
                 Lcd.lcdClear(lcdHandle); 
            }
        });
        
        try {
            setupWiringPi();
            lcdHandle = initLcd();  
        } catch (Exception ex) {}
        
        Gpio.pinMode(GPIO_PIN, Gpio.INPUT);
        Gpio.pullUpDnControl(GPIO_PIN, Gpio.PUD_DOWN);
        Gpio.delay(10);
        formatter = new SimpleDateFormat("HH:mm:ss");
        
        interrupt_Listener = new Interrupt_Listener();
        Gpio.wiringPiISR(GPIO_PIN, Gpio.INT_EDGE_FALLING, interrupt_Listener);
        
    }
    
    public String displayMenu() throws InterruptedException {
        // clear LCD
        Lcd.lcdClear(lcdHandle);
        
        //System.out.println("Showing Menu...");
        interrupt_Listener.interruptable_Thread = new Thread(){
            public void run() {
                //System.out.println("Thread Running");
                try {
                    while(true) {
                        selectedOption = F5BClient2.VARIO;
                        writeMenuOption(formatTextToFit1Line(F5BClient2.VARIO));
                        
                        selectedOption = F5BClient2.CLIENT;
                        writeMenuOption(formatTextToFit1Line(F5BClient2.CLIENT));
                        
                        selectedOption = F5BClient2.NETWORK_CHECK;
                        writeMenuOption(formatTextToFit1Line(F5BClient2.NETWORK_CHECK));
                        
                        selectedOption = F5BClient2.SCANNER;
                        writeMenuOption(formatTextToFit1Line(F5BClient2.SCANNER));
                        
                        selectedOption = F5BClient2.TIME;
                        writeMenuOption(formatTextToFit1Line(F5BClient2.TIME));
                        
                        selectedOption = F5BClient2.EXIT;
                        writeMenuOption(formatTextToFit1Line(F5BClient2.EXIT));
                        
                    }
                } catch (InterruptedException e) {
                    System.out.println("Interrupt in display Menu");
                } 
            }
        };
        interrupt_Listener.interruptable_Thread.start();
        
        try {
            interrupt_Listener.interruptable_Thread.join();
        } catch (InterruptedException e) {
            System.out.println("Interrupt in join of Display Menu");
        }
        //System.out.println("After join");
        return selectedOption;
    }
    
    public static String formatTextToFit1Line(String value) {
        if (value.length()>=16) {
            return value.substring(0, 16);
        } else {
            if ((value.length() % 2) != 0) {
                value = value + "!";
            }
            Integer numberOfSparePos = 16 - value.length();
            Integer halfOfNumberOfSparePostitions = numberOfSparePos / 2;
            if (halfOfNumberOfSparePostitions != 0) {
                value = " " + value + " ";
            }
            for (int i=2; i<= halfOfNumberOfSparePostitions; i++ ) {
                value = "-" + value + "-";
            }
            return value;
        }
    }
    
    public void writeLineWithDate(String value) throws InterruptedException{
        Lcd.lcdClear(lcdHandle);
        Lcd.lcdPosition (lcdHandle, 0, 0) ;
        Lcd.lcdPuts (lcdHandle, "--- " + formatter.format(new Date()) + " ---");
        Lcd.lcdPosition (lcdHandle, 0, 1) ;
        Lcd.lcdPuts (lcdHandle, value);
        Thread.sleep(1000);
    }
    
    public void write2Lines(String value1, String value2) throws InterruptedException {
        Lcd.lcdClear(lcdHandle);
        Lcd.lcdPosition (lcdHandle, 0, 0) ;
        Lcd.lcdPuts (lcdHandle, value1);
        Lcd.lcdPosition (lcdHandle, 0, 1) ;
        Lcd.lcdPuts (lcdHandle, value2);
        Thread.sleep(2000);
        
    }
    
    public void writeStop() throws InterruptedException {
        writeOneLineWithMinusBelow("--- STOPPING ---");
    }
    
    public void writeMenuOption(String value) throws InterruptedException {
        writeOneLineWithMinusBelow(value);
    }
    
    public void writeWelcome() throws InterruptedException {
        // clear LCD
        Lcd.lcdClear(lcdHandle);
        
        writeOneLineWithMinusBelow("-- F5B Client --");
        
        Lcd.lcdClear(lcdHandle);
    }
    
    private void writeOneLineWithMinusBelow(String text) throws InterruptedException {
        // write line 1 to LCD
        Lcd.lcdHome(lcdHandle);
        //Lcd.lcdPosition (lcdHandle, 0, 0) ; 
        Lcd.lcdPuts (lcdHandle, text) ;
        
        // write line 2 to LCD        
        Lcd.lcdPosition (lcdHandle, 0, 1) ; 
        Lcd.lcdPuts (lcdHandle, "----------------") ;
        Thread.sleep(2000);
    }
    
    public void varioTest() {
        SoftTone.softToneCreate(PIEZO_PIN);
        Lcd.lcdHome(lcdHandle);
        Lcd.lcdPosition (lcdHandle, 0, 0) ; 
        Lcd.lcdPuts (lcdHandle, "Vario Sounds....") ;
            
            
        int j=200;
        for (int i = 1; i < 20; ++i) {
            SoftTone.softToneWrite(PIEZO_PIN, j);
            //System.out.printf("%3d\n", j);
            j = j+100;
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
            
        SoftTone.softToneStop(PIEZO_PIN);
    }
    
    public void playVario (int updown) {
        Thread player = new Thread(){
            public void run() {
                //System.out.println("UPDOWN: " + updown);
                //System.out.println("PlayVario:" + System.currentTimeMillis());
                if(updown >= -4 && updown <= 4) {
                    //vario0();
                } else if (updown >= 5 && updown <= 8) {
                    varioPlus1();
                } else if (updown >= 9 && updown <= 10) {
                    varioPlus2();
                } else if (updown > 10) {
                    varioPlus3();
                } else if (updown <= -5 && updown >= -8) {
                    varioMin1();
                } else if (updown <= -9 && updown >= -10) {
                    varioMin2();
                } else if (updown < -10) {
                    varioMin3();
                } else {
                    //vario0();
                }
            }
        };
        player.start();
    }
            
    private void varioPlus1() {
        playLongSound(1600);
    }
    
    private void varioPlus2() {
        playLongSound(1900);
    }
    
    private void varioPlus3() {
        playShortSound(1900);
        playShortSound(1900);
    }
    
    private void varioMin1() {
        playLongSound(400);
    }
    
    private void varioMin2() {
        playLongSound(200);
    }
    
    private void varioMin3() {
        playShortSound(200);
        playShortSound(200);
    }
    
    private void playLongSound(int tone) {
        SoftTone.softToneCreate(PIEZO_PIN);
        SoftTone.softToneWrite(PIEZO_PIN, tone);
        try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
            }
        SoftTone.softToneStop(PIEZO_PIN);
        try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
    
    private void playShortSound(int tone) {
        SoftTone.softToneCreate(PIEZO_PIN);
        SoftTone.softToneWrite(PIEZO_PIN, tone);
        try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
            }
        SoftTone.softToneStop(PIEZO_PIN);
        try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Logger.getLogger(Handler.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
    
    public void clearScreen() throws InterruptedException {
        // clear LCD
        Lcd.lcdClear(lcdHandle);
    }
    
    private static void setupWiringPi() throws Exception {
        if (Gpio.wiringPiSetup() == -1) {
            System.out.println(" ==>> GPIO SETUP FAILED");
            Exception e = new Exception("GPIO SETUP FAILED");
            throw e;
        } else {
            System.out.println(" ==>> GPIO SETUP OK");        
            
        }
    }
    
    private static int initLcd() throws Exception {
        int lcdHandle = getLcdHandle();
        // verify initialization
        if (lcdHandle == -1) {
            System.out.println(" ==>> LCD INIT FAILED");
            Exception e = new Exception("LCD INIT FAILED");
            throw e;
        } else {
            System.out.println(" ==>> LCD INIT OK");        
        }
        return lcdHandle;
    }
    
    private static int getLcdHandle() {
        int lcdHandle= Lcd.lcdInit(LCD_ROWS,     // number of row supported by LCD
                                   LCD_COLUMNS,  // number of columns supported by LCD
                                   LCD_BITS,     // number of bits used to communicate to LCD 
                                   11,           // LCD RS pin
                                   10,           // LCD strobe pin
                                   0,            // LCD data bit 1
                                   1,            // LCD data bit 2
                                   2,            // LCD data bit 3
                                   3,            // LCD data bit 4
                                   0,            // LCD data bit 5 (set to 0 if using 4 bit communication)
                                   0,            // LCD data bit 6 (set to 0 if using 4 bit communication)
                                   0,            // LCD data bit 7 (set to 0 if using 4 bit communication)
                                   0);           // LCD data bit 8 (set to 0 if using 4 bit communication)
        return lcdHandle;
    }
}