package F5BClient2;

import F5BClient2.Pi_LCD_GPIO.Handler;
import com.pi4j.wiringpi.Lcd;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

public class SensorWebSocketClient extends Endpoint{
    private Session session;
    private final Handler lcd_gpio_Handler;
    private final SimpleDateFormat formatter;
    private String wMin;
    private String altitude;
    private long sensorReadTime;
    private int telemetryNOkCounter;
    private final int telemetryNotOkMaxCounter = 20;
    
    SensorWebSocketClient(Handler lcd_gpio_Handler) {
        //this.lcdHandle=lcdHandle;
        this.lcd_gpio_Handler = lcd_gpio_Handler;
        lcd_gpio_Handler.varioTest();
        telemetryNOkCounter = 0;
        formatter = new SimpleDateFormat("HH:mm:ss");
        wMin = "N/A";
        altitude = "N/A";
        sensorReadTime = System.currentTimeMillis()-10000;  //init to something 10 secs ago
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread2 = new Thread(){
            @Override
            public void run() {
                try {
                       //lcd_gpio_Handler.varioTest();
                       Thread.sleep(7000);
                    while (true) {
                        //System.out.println("TimeDiff: " + Long.toString(System.currentTimeMillis() - sensorReadTime));
                        if (System.currentTimeMillis() - sensorReadTime < 5000) {
                            //System.out.println("Updating screen with WMin");
                            Lcd.lcdClear(lcd_gpio_Handler.lcdHandle);
                            Lcd.lcdPosition (lcd_gpio_Handler.lcdHandle, 0, 0);
                            Lcd.lcdPuts (lcd_gpio_Handler.lcdHandle, Handler.formatTextToFit1Line("WMin:" + wMin));
                            Lcd.lcdPosition (lcd_gpio_Handler.lcdHandle, 0, 1);
                            Lcd.lcdPuts (lcd_gpio_Handler.lcdHandle, Handler.formatTextToFit1Line("Alt:" + altitude));
                            Thread.sleep(5000);
                        } else {
                            Lcd.lcdClear(lcd_gpio_Handler.lcdHandle);
                            Lcd.lcdPosition (lcd_gpio_Handler.lcdHandle, 0, 0);
                            Lcd.lcdPuts (lcd_gpio_Handler.lcdHandle, Handler.formatTextToFit1Line("Last WMin:" + wMin));
                            Lcd.lcdPosition (lcd_gpio_Handler.lcdHandle, 0, 1);
                            Lcd.lcdPuts (lcd_gpio_Handler.lcdHandle, Handler.formatTextToFit1Line("Sensor Dead"));
                            Thread.sleep(5000);
                            
                        }
                    }
                    
                } catch (InterruptedException e) {
                    System.out.println("Interrupted SensorWebSocketClient");
                }
            }
        };
        lcd_gpio_Handler.interrupt_Listener.interruptable_Thread2.start();
        //try {
        //    lcd_gpio_Handler.interrupt_Listener.interruptable_Thread2.join();
        //} catch (InterruptedException e) {
        //}
    }
    
    @Override
    public void onOpen(Session sn, EndpointConfig ec) {
        //System.out.println("SensorWebSocketClient opened");
        this.session = sn;
        this.session.addMessageHandler(new MessageHandler.Whole<byte[]>() {
            @Override
            public void onMessage(byte[] message) {
                sensorReadTime = System.currentTimeMillis();
                //System.out.println("MessageTime: " + sensorReadTime);
                String originalValue = "";
                int l = message.length;
                for(int i = 0; i < l; i++) {
                   originalValue = originalValue + message[i]; 
                }
                //System.out.println("Message: " + originalValue);  
                
                ByteBuffer unescapedMessage = unesc(message);
                unescapedMessage.flip();
                unescapedMessage.rewind();
                byte[] sens1 = new byte[3];
                byte[] sens2 = new byte[3];
                SensorReading sensValue1 = null;
                SensorReading sensValue2 = null;
                if (l > 19 && message[0] == 0x02 && message[l - 1] == 0x03 && checks(unescapedMessage)) {
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
        });
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
                    //System.out.println("VARIO Message: " + sensorReadTime);
                    break;
                case 12:
                    wMin = Integer.toString(sensorReading.sensorValue);
                    //System.out.println("WMIN Message: " + sensorReadTime);
                    break;
                case 8:
                    altitude = Integer.toString(sensorReading.sensorValue);
                    //System.out.println("ALT Message: " + sensorReadTime);
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
