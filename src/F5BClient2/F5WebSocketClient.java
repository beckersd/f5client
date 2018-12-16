package F5BClient2;

import F5BClient2.Pi_LCD_GPIO.Handler;
import com.pi4j.wiringpi.Lcd;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

public class F5WebSocketClient extends Endpoint{
    private Session session;
    private final int lcdHandle;
    private final Handler lcd_gpio_Handler;

    F5WebSocketClient(int lcdHandle, Handler lcdHandler) {
         this.lcdHandle=lcdHandle;
         this.lcd_gpio_Handler=lcdHandler;
    }
    
    @Override
    public void onOpen(Session sn, EndpointConfig ec) {
        //System.out.println("WebSocketClient.onOpen()");
        this.session = sn;
        
        this.session.addMessageHandler(new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String message) {
                //System.out.println("Message: " + message);
                //splitting message in pilot, time and laps
                String[] separated = message.split("/");
                //System.out.println("Message0: " + separated[0]);
                //System.out.println("Message1: $" + separated[1] + "$");
                String secs = separated[1].trim();
                //System.out.println("Secs: $" + secs + "$");
                int secsInt = Integer.parseInt(secs);
                //System.out.println("Secs: $" + secsInt + "$");
                if (secsInt == 195) {
                    //System.out.println("!!195!!");
                    lcd_gpio_Handler.varioMin3();
                }
                if (secsInt == 198) {
                    //System.out.println("!!198!!");
                    lcd_gpio_Handler.varioMin3();
                }
                if (secsInt == 200) {
                    //System.out.println("!!200!!");
                    lcd_gpio_Handler.varioPlus3();
                }
                //System.out.println("Message2: " + separated[2]);
                
                Lcd.lcdClear(lcdHandle);
                Lcd.lcdPosition (lcdHandle, 0, 0) ; 
                Lcd.lcdPuts (lcdHandle, separated[0].trim());
                Lcd.lcdPosition (lcdHandle, 0, 1) ; 
                Lcd.lcdPuts (lcdHandle, ("   " + separated[1].trim()).substring(("   " + separated[1].trim()).length()-3) + " / " + 
                        ("  " + separated[2].trim()).substring(("  " + separated[2].trim()).length()-2) + "  " + 
                        ("      " + separated[3].trim()).substring(("      " + separated[3].trim()).length()-6));
            }
        });
    }
}
