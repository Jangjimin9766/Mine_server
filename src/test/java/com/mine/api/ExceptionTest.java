import java.net.ConnectException;

public class ExceptionTest {
    public static void main(String[] args) {
        System.out.println("NullPointerException: " + new NullPointerException().getMessage());
        
        try {
            java.util.Map.of("input", null);
        } catch (Exception e) {
            System.out.println("Map.of NPE: " + e.getMessage());
        }
        
    }
}
