package F5BClient2;

import F5BClient2.Pi_LCD_GPIO.Handler;
import com.pi4j.wiringpi.Lcd;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.Session;
import purejavacomm.CommPortIdentifier;
import purejavacomm.NoSuchPortException;
import purejavacomm.PortInUseException;
import purejavacomm.SerialPort;
import purejavacomm.SerialPortEvent;
import purejavacomm.SerialPortEventListener;
import purejavacomm.UnsupportedCommOperationException;

public class SensorPureJavaCommClient {
    private Session session;
    private final Handler lcd_gpio_Handler;
    private final SimpleDateFormat formatter;
    private String wMin;
    private String altitude;
    private long sensorReadTime;
    private int telemetryNOkCounter;
    public boolean serialConnectOk = false;
    private final int telemetryNotOkMaxCounter = 20;
    private final long screenUpdateInterval = 3000;
    byte[] message;
    String originalValue;
    
    private SerialPort serialPort;
    private CommPortIdentifier portId;
    private InputStream inputStr;
    private int length;
    
    SensorPureJavaCommClient(Handler lcd_gpio_Handler) {
        //this.lcdHandle=lcdHandle;
        this.lcd_gpio_Handler = lcd_gpio_Handler;
        telemetryNOkCounter = 0;
        formatter = new SimpleDateFormat("HH:mm:ss");
        wMin = "N/A";
        altitude = "N/A";
        serialConnectOk = false;
        sensorReadTime = System.currentTimeMillis()-10000;  //init to something 10 secs ago
    }
    
    public void start() {
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread = new Thread(){
            @Override
            public void run() {
                try {
                    while (true) {
                        while (!serialConnectOk) {
                            lcd_gpio_Handler.writeLineWithDate("Connecting Radio");
                            try {
                                portId = CommPortIdentifier.getPortIdentifier("/dev/ttyUSB0");
                            } catch (NoSuchPortException ex) {
                                Logger.getLogger(SensorPureJavaCommClient.class.getName()).log(Level.SEVERE, null, ex);
                                System.out.println("Unable to open port: " + ex.toString());
                                Thread.sleep(1000);
                            }
                            if (portId != null) {
                                System.out.println("Opening USB Port...");
                                serialPort = (SerialPort) portId.open("MPXSerial", 2000);
                                System.out.println("USB Port Open!");

                                serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_XONXOFF_IN+SerialPort.FLOWCONTROL_XONXOFF_OUT);
                                serialPort.setSerialPortParams(115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                                inputStr = serialPort.getInputStream();
                                serialPort.notifyOnDataAvailable(true);
                                serialConnectOk = true;
                            }

                            serialPort.addEventListener(new SerialPortEventListener() {
                                @Override                                
                                public void serialEvent(SerialPortEvent event) {
                                    //System.out.println("Portevent detected: " + event.getEventType());
                                    try {
                                        switch (event.getEventType() ) {
                                            case SerialPortEvent.DATA_AVAILABLE: 
                                                System.out.println("Data Available");
                                                String data = "";
                                                length = inputStr.available();
                                                if (length > 0) {
                                                    sensorReadTime = System.currentTimeMillis();
                                                    message = new byte[length];
                                                    inputStr.read(message);
                                                    System.out.println(System.currentTimeMillis() + "Data length: " + length + " / Data: " + message.toString());

                                                    ByteBuffer unescapedMessage = unesc(message);
                                                    unescapedMessage.flip();
                                                    unescapedMessage.rewind();
                                                    byte[] sens1 = new byte[3];
                                                    byte[] sens2 = new byte[3];
                                                    SensorReading sensValue1 = null;
                                                    SensorReading sensValue2 = null;
                                                    if (length > 19 && message[0] == 0x02 && message[length - 1] == 0x03 && checks(unescapedMessage)) {
                                                        //System.out.println("Message OK");
                                                        unescapedMessage.rewind();
                                                        if (message[7] == 6) {
                                                            //System.out.println("7th position of original message = 6, so telemetry is being received by transmitter");
                                                            unescapedMessage.position(8);
                                                            //Long when = timBytes.getWhen();
                                                            unescapedMessage.get(sens1, 0, 3);
                                                            sensValue1 = treatSensor(sens1);
                                                            unescapedMessage.get(sens2, 0, 3);
                                                            sensValue2 = treatSensor(sens2);
                                                        } else {
                                                            //System.out.println("No telemetry from receiver!");
                                                            telemetryNotOkSetter();
                                                        }
                                                    } else {
                                                        //System.out.println("Message NOT OK");
                                                        telemetryNotOkSetter();
                                                    }    
                                                }
                                                break;
                                            default:
                                                System.out.println("No data available");
                                                break;
                                        }
                                    } 
                                    catch (Exception e) {
                                        System.out.println("Error reading data: " + e.toString());
                                        serialConnectOk = false;
                                        //e.printStackTrace();
                                    }
                                }                    
                            });
                        
                            //System.out.println("TimeDiff: " + Long.toString(System.currentTimeMillis() - sensorReadTime));
                            if (System.currentTimeMillis() - sensorReadTime < 2000) {
                                //System.out.println("Updating screen with WMin/Alt");
                                Lcd.lcdClear(lcd_gpio_Handler.lcdHandle);
                                Lcd.lcdPosition (lcd_gpio_Handler.lcdHandle, 0, 0);
                                Lcd.lcdPuts (lcd_gpio_Handler.lcdHandle, Handler.formatTextToFit1Line("WMin:" + wMin));
                                Lcd.lcdPosition (lcd_gpio_Handler.lcdHandle, 0, 1);
                                Lcd.lcdPuts (lcd_gpio_Handler.lcdHandle, Handler.formatTextToFit1Line("Alt:" + altitude));
                                Thread.sleep(screenUpdateInterval);
                            } else {
                                Lcd.lcdClear(lcd_gpio_Handler.lcdHandle);
                                Lcd.lcdPosition (lcd_gpio_Handler.lcdHandle, 0, 0);
                                Lcd.lcdPuts (lcd_gpio_Handler.lcdHandle, Handler.formatTextToFit1Line("Last WMin:" + wMin));
                                Lcd.lcdPosition (lcd_gpio_Handler.lcdHandle, 0, 1);
                                Lcd.lcdPuts (lcd_gpio_Handler.lcdHandle, Handler.formatTextToFit1Line("Sensor Dead"));
                                Thread.sleep(screenUpdateInterval);
                            }
                        }
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(SensorPureJavaCommClient.class.getName()).log(Level.SEVERE, null, ex);
                } catch (PortInUseException ex) {
                    System.out.println("Port is in use");
                    serialConnectOk = false;
                } catch (TooManyListenersException ex) {
                    System.out.println("Too Many Listeners on port");
                    serialConnectOk = false;
                } catch (UnsupportedCommOperationException ex) {
                    System.out.println("Unsupported communication operation");
                    serialConnectOk = false;
                } catch (IOException ex) {
                    System.out.println("IOException");
                    serialConnectOk = false;
                }   
            }   
        };
        
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread.start();
        try {
            lcd_gpio_Handler.interrupt_Listener.interruptable_Thread.join();
        } catch (InterruptedException e) {
        }
    }      
               
            
    private void telemetryNotOkSetter() {
        telemetryNOkCounter++;
        //System.out.println("No telemetry detected: " + telemetryNOkCounter + " of maximum " + telemetryNotOkMaxCounter);
        if (telemetryNOkCounter > telemetryNotOkMaxCounter) {
            altitude = "N/A";
            telemetryNOkCounter = 0;
        }
    }
    
    private SensorReading treatSensor(byte[] s) {

        int addr=(int)((s[0]&0xff)>>>4);   // so byte[0] is type (between 0 and 13
        if (addr<0 || addr>15) return null;   //"addr not between 0 and 15... whatever that would mean..."
        int cls=(int)(0xf & s[0]);
        if (cls<=0 || cls>13) return null;   //"cls not between 0 and 13"
        
        short low=(short)(0xfe&s[1]);         //byte[1] = low value of the sensor
        short high=(short)((0xff&s[2])<<8);   //byte[2] = high value of the sensor
        
        short value = (short)((high|low)>>1); //the actual sensor value is the rightshift1 of high or low
        boolean valid = false;
        if (Math.abs(value)<16000) {
            valid = true;
                
            SensorReading sensorReading = new SensorReading(cls, value);

            switch (sensorReading.sensorType) {
                case 3:
                    lcd_gpio_Handler.playVario(sensorReading.sensorValue);
                    //System.out.println("VARIO Message: " + sensorReadTime + " // " + sensorReading.sensorValue);
                    break;
                case 12:
                    wMin = Integer.toString(sensorReading.sensorValue);
                    //System.out.println("WMIN Message: " + sensorReadTime + " // " + sensorReading.sensorValue);
                    break;
                case 8:
                    altitude = Integer.toString(sensorReading.sensorValue);
                    //System.out.println("ALT Message: " + sensorReadTime + " // " + sensorReading.sensorValue);
                    break;
            }
            return sensorReading;
        } else {
            return null;
        }
    }
    
    private static ByteBuffer unesc(byte[] buf){
        ByteBuffer u=ByteBuffer.allocate(buf.length);
        int i=0;
        int l=buf.length;
        while (i<l){
            byte b=buf[i];
            if ((b == 0x1b) && i<l-1) {
                i++;
                u.put((byte)(buf[i] & 0x1b));
            } else u.put(b);
            i++;
        }
        return u;
    }
    
    private static boolean checks(ByteBuffer record){
        byte[] ar=record.array();
        int l=record.limit();
        int check=0;
        for (int i=1;i<l-2;i++){
            int x=(0xff & ar[i]);
            check+=x;
        }
        check=~check;
        byte c=ar[l-2];
        return (byte)(0xff & check) == c;
    }
        
    private class SensorReading {
        private final int sensorType;
        private final int sensorValue;
        
        SensorReading(int sensorType, int sensorValue) {
            this.sensorType = sensorType;
            this.sensorValue = sensorValue;
        }
        
        @Override
        public String toString() {
            return this.sensorTypeString() + ": " + this.sensorValue;
        }
        
        public String sensorTypeString() {
            switch (this.sensorType) {
                case 0:
                    return "?";
                case 1:
                    return "V";
                case 2:
                    return "A";
                case 3:
                    return "m/s";
                case 4:
                    return "km/h";
                case 5:
                    return "t/min";
                case 6:
                    return " Â°C";
                case 7:
                    return " Â° ";
                case 8:
                    return "m";
                 case 9:
                    return "%";
                case 10:
                    return "%LQI";
                case 11:
                    return "mAh";
                case 12:
                    return "ml";  // Should be the WMin
                case 13:
                    return "km";
                default:
                    return "?";
            }   
        }
            
    }
    
}
