package F5BClient2.Pi_LCD_GPIO;

import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioInterruptCallback;

public class Interrupt_Listener implements GpioInterruptCallback{

    public Thread interruptable_Thread;
    public Thread interruptable_Thread2;

    private final long debounceTime = 200;
    
    private long lastTime;
    
    public Interrupt_Listener() {
        System.out.println("Interruplistener initialized");
    }
    
    @Override
    public void callback(int pin) {
        long currentTime = System.currentTimeMillis();
        if(currentTime > lastTime+debounceTime){
            Gpio.digitalWrite(pin, Gpio.digitalRead(pin)==0?1:0);                
                        
            System.out.println("Button Pressed");
            //System.out.println("Thread to interrupt: " + interruptable_Thread.getName());
            //while (interruptable_Thread.isAlive()) {
            //System.out.println("Interrupting thread...");
            if (interruptable_Thread != null) {
                //System.out.println("INTERRUPT THREAD 1");
                interruptable_Thread.interrupt();
            }
            if (interruptable_Thread2 != null) {
                //System.out.println("INTERRUPT THREAD 2");
                interruptable_Thread2.interrupt();
            }
            
        } else {
            System.out.println("Discard event "+currentTime);
        }              
        lastTime=currentTime;
    }
    
}
