package F5BClient2;

public class F5BClient2Runner {
    
    private static final String VERSION = "v2.4"; 
    
    public static void main(String[] args) throws Exception {
        F5BClient2 mainApp = new F5BClient2();
        mainApp.start(VERSION);
    }
}
