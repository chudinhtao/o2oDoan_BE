import java.sql.*;

public class CheckDb {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5433/fnb_db", "fnb_user", "fnb_pass");
        Statement st = conn.createStatement();
        try {
            ResultSet rs = st.executeQuery("SELECT id, name, is_active, start_at, end_at, now() FROM menu.promotions");
            while(rs.next()) {
                System.out.println("PROMO: " + rs.getString("name") + " | Active: " + rs.getBoolean("is_active") + " | Start: " + rs.getString("start_at") + " | End: " + rs.getString("end_at") + " | Now: " + rs.getString("now"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
