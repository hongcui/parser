/**
 * 
 */
package fna.ID;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;
import fna.parsing.ApplicationUtilities;

/**
 * @author Hong Updates
 * this class assign a type of IDs to the rows of a database table, which must have an "id" field
 *
 */
public class TermIDAssigner {
	private Connection conn = null;
	private static String url = ApplicationUtilities.getProperty("database.url");
	private String schemaname;
	private String tablename;
	private String idcolumn;
	
	public TermIDAssigner(String schemaname, String tablename, String idcolumn){
		this.tablename = tablename;
		this.idcolumn = idcolumn;
		this.schemaname = schemaname;
		try{
			Class.forName("com.mysql.jdbc.Driver");
			this.conn = DriverManager.getConnection(url);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void assignID(){
		if(this.conn != null){
			try{
				Statement stmt = this.conn.createStatement();
				String q = "SELECT count(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE " +
						"TABLE_SCHEMA='"+this.schemaname+"' AND TABLE_NAME = '"+tablename+"' AND COLUMN_NAME = '"+idcolumn+"'";
				ResultSet rs = stmt.executeQuery(q);
				rs.next();
				if(rs.getInt(1)==0){
					q = "alter table "+schemaname+"."+tablename+" add column "+idcolumn+" varchar(200) default ''";
					stmt.execute(q);
				}
				rs = stmt.executeQuery("select id from "+tablename);
				while(rs.next()){
					int id = rs.getInt("id");
					String uuid = UUID.randomUUID().toString();
					Statement stmt1 = this.conn.createStatement();
					stmt1.execute("update "+schemaname+"."+tablename+" set "+idcolumn+"='"+uuid+"' where id="+id);
				}								
			}catch(Exception e){
					e.printStackTrace();
			}			
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TermIDAssigner tia = new TermIDAssigner("annotationevaluation", "fnaglossaryfixed", "FNA_ID");
		tia.assignID();		
	}

}
