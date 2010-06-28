package fna.charactermarkup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

public class SegmentIntegrator {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "";
	
	public SegmentIntegrator(String database) {
		// TODO Auto-generated constructor stub
		collect(database);
	}
	
	protected void collect(String database){
		SegmentIntegrator.database = database;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				integrator();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void integrator() {
		try{
			Statement stmt = conn.createStatement();
			String[] sentid = new String[30];
			String newsrcstr = "", oldsrcstr = "";
			int srcflag = 0, ct = 0;
			/////
			ResultSet rs = stmt.executeQuery("select * from marked_simpleseg where source='1.txt-3'");
        	while(rs.next()){
        		newsrcstr=rs.getString("source");
        		if(oldsrcstr.compareTo(newsrcstr)==0)
        			srcflag = 1;
        		else
        			srcflag = 0;
        		if(srcflag == 1){
        			sentid[ct] = rs.getString("sentid");
        			ct++;
        			//////
        			XMLintegrator(sentid, ct, oldsrcstr);
        		}
        		else{
        			/////Change the "10" to "1"
        			if(rs.getString("sentid").compareTo("10")==0){
        				sentid[ct] = rs.getString("sentid");
        				ct++;
        			}
        			else{
        				XMLintegrator(sentid, ct, oldsrcstr);
        				for(int i = 0; i < sentid.length; i++)
        					sentid[i] = "";
        				ct = 0;
        				sentid[ct] = rs.getString("sentid");
        				ct++;
        			}
        			
        		}
        		oldsrcstr=rs.getString("source");
        	}
		
		
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	private void XMLintegrator(String[] sentid, int ct, String source) {
		try{
			SAXBuilder builder = new SAXBuilder();
			Document testcase = builder.build("F:\\UA\\RA\\TestCase_Benchmark\\"+sentid[0]+".xml");
			Element testroot = testcase.getRootElement();
			for(int i = 1; i < ct; i++){
				System.out.println(sentid[0]);
				Document next = builder.build("F:\\UA\\RA\\TestCase_Benchmark\\"+sentid[i]+".xml");
				Element nextroot = next.getRootElement();
				List testli = testroot.getChildren("structure");
				List nextli = nextroot.getChildren("structure");
				List testrel = testroot.getChildren("relation");
				int flag = 0;
				for(int j = 0; j < testli.size(); j++){ 
					if(nextli.get(0).toString().compareTo(testli.get(j).toString())==0){
						flag = 1;
						Element nextele = (Element)nextli.get(0);
						Element testele = (Element)testli.get(j);
						testele.addContent(nextele.getChildren());
						String id = testele.getAttributeValue("id");
						List nextrel = nextroot.getChildren("relation");
						for(int k = 0; k < nextrel.size(); k++){
							Element nextrelele = (Element)nextrel.get(k);
							if(nextrelele.getAttributeValue("from").compareTo("o1")==0){
								nextrelele.setAttribute("from", id);
							}
						}
						System.out.println(nextroot.getChildren().toString());
						System.out.println(nextroot.getChild("relation").getAttributes().toString());
						List nextlistruct = nextroot.getChildren("structure");
						for(int l = 1; l < nextlistruct.size(); l++){
							Element nextstructele = (Element)nextlistruct.get(l);
							nextstructele.setAttribute("id", "o"+(l+testli.size()));
							List nextlirel = nextroot.getChildren("relation");
							for(int m = 0; m < nextlirel.size(); m++){
								Element nextlirelele = (Element)nextlirel.get(m);
								if(nextlirelele.getAttributeValue("from").compareTo("o"+(l+1))==0){
									nextlirelele.setAttribute("from", "o"+(l+testli.size()));
								}
								else if(nextlirelele.getAttributeValue("to").compareTo("o"+(l+1))==0){
									nextlirelele.setAttribute("to", "o"+(l+testli.size()));
								}	
							}
						}
						nextrel = nextroot.getChildren("relation");
						for(int n = 0; n < nextrel.size(); n++){
							Element nextrelele = (Element)nextrel.get(n);
							nextrelele.setAttribute("id", "R"+(n+1+testrel.size()));
						}
						nextroot.removeChild("structure");
						testroot.addContent(nextroot.cloneContent().toString());
						break;
					}
				}
				if(flag == 0){
					List nextlistruct = nextroot.getChildren("structure");
					for(int l = 0; l < nextlistruct.size(); l++){
						Element nextstructele = (Element)nextlistruct.get(l);
						nextstructele.setAttribute("id", "o"+(l+1+testli.size()));
						List nextlirel = nextroot.getChildren("relation");
						for(int m = 0; m < nextlirel.size(); m++){
							Element nextlirelele = (Element)nextlirel.get(m);
							if(nextlirelele.getAttributeValue("from").compareTo("o"+(l+1))==0){
								nextlirelele.setAttribute("from", "o"+(l+1+testli.size()));
							}
							else if(nextlirelele.getAttributeValue("to").compareTo("o"+(l+1))==0){
								nextlirelele.setAttribute("to", "o"+(l+1+testli.size()));
							}	
						}
					}
					List nextrel = nextroot.getChildren("relation");
					for(int n = 0; n < nextrel.size(); n++){
						Element nextrelele = (Element)nextrel.get(n);
						nextrelele.setAttribute("id", "R"+(n+1+testrel.size()));
					}
					testroot.addContent(nextroot.cloneContent());
				}
			}
			FileOutputStream ostream = new FileOutputStream("F:\\UA\\RA\\TestCase_Benchmark_sentence\\"+source+".xml");
    		PrintStream out = new PrintStream( ostream );
			out.println("<?xml version=\"1.0\" encoding=\"iso8859-1\"?>");
			
			out.println(testroot.getContent());
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new SegmentIntegrator("benchmark_learningcurve_fnav19_test_24");
	}

}
