package F5BClient2.Pi_LCD_GPIO;

import org.junit.Test;
import static org.junit.Assert.*;

public class HandlerTest {
    
    public HandlerTest() {
    }

        /**
     * Test of formatTextToFit1Line method, of class Handler.
     */
    @Test
    public void testFormatTextToFit1Line() {
        System.out.println("formatTextToFit1Line");
        String value = "Dieter";
        String expResult = "---- Dieter ----";
        String result = Handler.formatTextToFit1Line(value);
        assertEquals(expResult, result);
        
        value = "ThisItTooLongForLine";
        expResult = "ThisItTooLongFor";
        result = Handler.formatTextToFit1Line(value);
        assertEquals(expResult, result);
        
        value = "ThisItTooLongFo";
        expResult = "ThisItTooLongFo!";
        result = Handler.formatTextToFit1Line(value);
        assertEquals(expResult, result);
        
        value = "ThisItTooLongF";
        expResult = " ThisItTooLongF ";
        result = Handler.formatTextToFit1Line(value);
        assertEquals(expResult, result);
        
         value = "Dieter1";
        expResult = "--- Dieter1! ---";
        result = Handler.formatTextToFit1Line(value);
        assertEquals(expResult, result);

    }
}
