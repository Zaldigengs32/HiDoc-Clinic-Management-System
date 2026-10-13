package Connection;

import java.sql.*;

public class DBConnect {
    public Connection conn;
    public Statement stat;
    public ResultSet result;
    public PreparedStatement pstat;
    public DBConnect() {
        try {
            String url = "jdbc:sqlserver://localhost:1433;databaseName=HiDocProject;user=sa;password=123;encrypt=true;trustServerCertificate=true";
            conn = DriverManager.getConnection(url);
            stat = conn.createStatement();
            System.out.println("Connection Berhasil");
        } catch (Exception e) {
            System.out.println("Error saat connect database: " + e);
        }
    }

    public static void main(String[] args) {
        DBConnect db = new DBConnect();
    }
}
