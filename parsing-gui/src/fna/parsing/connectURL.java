package fna.parsing;
import java.sql.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;


public class connectURL {

   public static void main(String[] args) {

      // Create a variable for the connection string.
     /* String connectionUrl = "jdbc:sqlserver://localhost\\MSSQLSERVER:1433;" +
    		  "databaseName=EFlora;user=termsuser;password=termspassword";*/
	 String connectionUrl = "jdbc:sqlserver://localhost:1433;Instance=MSSQLSERVER;user=termsuser;password=termspassword";
	   
	   
      // Declare the JDBC objects.
      Connection con = null;
      Statement stmt = null;
      ResultSet rs = null;

      try {
         // Establish the connection.
         Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
         
         con = DriverManager.getConnection(connectionUrl);

         // Create and execute an SQL statement that returns some data.
         String SQL = "SELECT TOP 10 * FROM Flora";
         stmt = con.createStatement();
         rs = stmt.executeQuery(SQL);

         // Iterate through the data in the result set and display it.
         while (rs.next()) {
            System.out.println(rs.getString(2) + " " + rs.getString(6));
         }
      }

      // Handle any errors that may have occurred.
      catch (Exception e) {
         e.printStackTrace();
      }
      finally {
         if (rs != null) try { rs.close(); } catch(Exception e) {}
         if (stmt != null) try { stmt.close(); } catch(Exception e) {}
         if (con != null) try { con.close(); } catch(Exception e) {}
      }
   }
}