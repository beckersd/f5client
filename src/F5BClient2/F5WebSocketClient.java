package F5BClient2;

import com.pi4j.wiringpi.Lcd;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

public class F5WebSocketClient extends Endpoint{
    private Session session;
    private final int lcdHandle;

    F5WebSocketClient(int lcdHandle) {
         this.lcdHandle=lcdHandle;
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
                //System.out.println("Message1: " + separated[1]);
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
