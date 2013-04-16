package input;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.NodeList;

import fna.parsing.ApplicationUtilities;

public class ExtractAntStructures {

	public static void main(String[] args) {
		try {
			DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
			domFactory.setNamespaceAware(true);
			DocumentBuilder builder = null;
			try{
				builder = domFactory.newDocumentBuilder();
			}catch(Exception e){
				e.printStackTrace();
				}
			Connection con = null;
			try {
				Class.forName("com.mysql.jdbc.Driver").newInstance();
				con = DriverManager.getConnection("jdbc:mysql://localhost/test","root", "sonali");
				} catch (Exception e) {
				e.printStackTrace();
			}
						
			String path1 = "C:/Users/sonaliranade/Desktop/Ant_files1";
			String files1 = "";
			File folder1 = new File(path1);
			File[] listOfFiles1 = folder1.listFiles();
						
			//for loop for each folder in the Ants folder
			for (int i = 0; i < listOfFiles1.length; i++) {
				
				if (listOfFiles1[i].isDirectory()) {
					files1 = listOfFiles1[i].getName();
					System.out.println("");
					System.out.println("");
					System.out.println(files1);
					System.out.println("");
					System.out.println("");
					String path2 = "C:/Users/sonaliranade/Desktop/Ant_files1/" + files1;
					String files2 = "";
					File folder2 = new File(path2);
					File[] listOfFiles2 = folder2.listFiles();
					
					//for folders in current folder
					for (int j = 0; j < listOfFiles2.length; j++) {
						if (listOfFiles2[j].isDirectory()) {
							files2 = listOfFiles2[j].getName();
							//System.out.print(files2 + "\t");
							if (files2.equals("target")){
								String path3 = "C:/Users/sonaliranade/Desktop/Ant_files1/" + files1 + "/target";
								String files3 = "";
								File folder3 = new File(path3);
								File[] listOfFiles3 = folder3.listFiles();
								//for folders in target folder
								for (int k = 0; k < listOfFiles3.length; k++) {
									if (listOfFiles3[k].isDirectory()) {
										files3 = listOfFiles3[k].getName();
										//System.out.print(files3 + "\t");
										if (files3.equals("final")){
											//String path4 = "C:/Users/sonaliranade/Desktop/Ant_files/" + files1 + "target";
											String path4 = path3 + "/final/";
											String files4 = "";
											File folder4 = new File(path4);
											File[] listOfFiles4 = folder4.listFiles();
											//for files in final folder
											for (int l = 0; l < listOfFiles4.length; l++) {
												files4 = listOfFiles4[l].getName();
												System.out.println();
												System.out.println();
												System.out.println(files4);
												System.out.println("");
												System.out.println("");
												Document doc1 = null; 
												try{
													doc1 = builder.parse(path4 + files4);													
												}catch(Exception e){
													e.printStackTrace();
													}
																															
												XPath xpath1 = XPathFactory.newInstance().newXPath();
												XPathExpression expr1 = null;
												
												try{
													//selects all attribute nodes under all structure nodes 
													expr1 = xpath1.compile("//structure/@*");
													}catch(Exception e){
													e.printStackTrace();
													}
												Object result1 = null;
												
												try {
													result1 = expr1.evaluate(doc1, XPathConstants.NODESET);
													
												}catch(Exception e){
													e.printStackTrace();
													}
												NodeList nodes1 = (NodeList) result1;
												
												int length = nodes1.getLength();
												String constraint = "";
												String structure = "";
												//for each node in the NodeList nodes1
												for( int m=0; m<length; m++) {
												    Attr attr = (Attr) nodes1.item(m);
												    String name = attr.getName();
												    if (name.equals("constraint")== true){
												    	constraint = attr.getValue();
												    	constraint = constraint + "_";
													    //System.out.println(constraint);
												    }
												    if (name.equals("name")== true){
												    	structure = attr.getValue();
												    	structure = constraint + structure;
												    	constraint = "";
													    System.out.println(structure);
													    Statement stmt = null;
													    try {
															if (!con.isClosed()) {
																stmt = con.createStatement();
																String sql =  "Insert into ant_structure values ('" + structure + "')";
															    stmt.execute(sql);
																}
														}catch(SQLException e) {
																	e.printStackTrace();
														}finally{
															try{
																if(stmt!=null) stmt.close();
															}catch(Exception e){
																e.printStackTrace();
															}
														}
												    }
												    
												}
												
											}
										}
									}
								}
							}
						}
					}
				}
			}

		}finally{
		
			}
	}
}


