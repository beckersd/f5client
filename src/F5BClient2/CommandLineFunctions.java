package F5BClient2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class CommandLineFunctions {
    
    public static String executeCommand(String command) {

	StringBuilder output = new StringBuilder();

	Process p;
	try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine())!= null) {
                StringBuilder append = output.append(line).append("\n");
            }

        } catch (IOException | InterruptedException e) {
            System.out.println("Error in Execute command...");
            //e.printStackTrace();
	}
	return output.toString();

    }
    
}
