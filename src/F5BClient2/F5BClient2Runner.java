package F5BClient2;

public class F5BClient2Runner {
    
    private static final String version = "v2.0"; 
    
    public static void main(String[] args) throws Exception {
        F5BClient2 mainApp = new F5BClient2();
        mainApp.start(version);
    }
}
