import java.sql.Connection;
import java.sql.DriverManager;

public class TestOracle {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@//localhost:1521/FREEPDB1", "system", "oracle123");
            System.out.println("Success: " + conn.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
