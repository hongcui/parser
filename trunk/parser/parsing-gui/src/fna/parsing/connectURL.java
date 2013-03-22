package fna.parsing;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;


public class connectURL {
	 private static final Logger LOGGER = Logger.getLogger(connectURL.class);  


   public static void main(String[] args) {

      // Create a variable for the connection string.
     /* String connectionUrl = "jdbc:sqlserver://localhost\\MSSQLSERVER:1433;" +
    		  "databaseName=EFlora;user=ApplicationUtilities.getProperty("database.username");password=termspassword";*/
	 String connectionUrl = ApplicationUtilities.getProperty("database.url");
	   
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
         StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
      }
      finally {
         if (rs != null) try { rs.close(); } catch(Exception e) {}
         if (stmt != null) try { stmt.close(); } catch(Exception e) {}
         if (con != null) try { con.close(); } catch(Exception e) {}
      }
   }
}