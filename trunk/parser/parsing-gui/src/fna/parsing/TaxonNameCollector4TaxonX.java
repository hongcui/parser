package fna.parsing;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

public class TaxonNameCollector4TaxonX extends TaxonNameCollector {
	private String volume;
	private File transformeddir;
	private String outputtablename;
	private Connection conn;
	private PreparedStatement insert;
	private TreeSet<String> names = new TreeSet<String>();
	 private static final Logger LOGGER = Logger.getLogger(TaxonNameCollector4TaxonX.class);  


	public TaxonNameCollector4TaxonX(Connection conn, String transformeddir,
			String outputtablename, String volume) throws Exception {
		super(conn, transformeddir, outputtablename, volume);
		// TODO Auto-generated constructor stub
		this.transformeddir = new File(transformeddir);
		this.outputtablename = outputtablename;
		this.conn = conn;
		this.volume = volume;
		Statement st = conn.createStatement();
		st.execute("drop table if exists "+this.outputtablename);
		PreparedStatement stmt = conn.prepareStatement("create table if not exists "+this.outputtablename+" (nameid MEDIUMINT not null auto_increment primary key, name varchar(100), source varchar(50))");
		stmt.execute();
		this.insert = conn.prepareStatement("insert into "+this.outputtablename+"(name, source) values (?, ?)");
	}
	
	public void collect4TaxonX(){
		try{
			File[] xmlfiles = this.transformeddir.listFiles();
			for(File xmlfile: xmlfiles){
				collectNames(xmlfile);
			}
			saveNames();
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
	}
	
	private void saveNames() {
		try{
			for(String name: names){
				name = name.replaceAll("\\.", "");
				if(name.length()>2){
					insert.setString(1, name);	
					insert.setString(2, this.volume);
					insert.execute();
				}
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}	
		
	}

	@SuppressWarnings("unchecked")
	private void collectNames(File xmlfile) {
		try{
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(xmlfile);
			Element root = doc.getRootElement();
			//List<Element> names = XPath.selectNodes(root, "tax:taxonxBody/tax:treatment/tax:div/description/tax:name");
			List<Element> names = XPath.selectNodes(root, "//tax:name");
			//System.out.println(xmlfile.getAbsolutePath());
			addNames(names);
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
		
	}

	/**
	 * if this.outputtablename exists, add the names to it
	 * else create a table
	 * @param names
	 * @param nametype
	 */
	private void addNames(List<Element> names) {
		try{
			for(Element name: names){
				String namerank = name.getName();
				String namestr = name.getTextTrim();
				List<Element> localElements = name.getChildren();
				for(Element local: localElements){
					List contentlist = local.getChildren();
					Iterator contentiter = contentlist.iterator();
					String text = "";
					while(contentiter.hasNext()){
						Element te = (Element)contentiter.next();
							text = text + te.getText();	
							String[] synchunks = new String[2];
							synchunks=text.split("\\s");
							for(int x=0;x<synchunks.length;x++)
							{
								String localname =synchunks[x].trim().toString().toLowerCase();
								this.names.add(localname);
							}		
					}
				}
				
				//There can be text within the name tag itself. If it is the case then add that too.
				if(namestr.trim().length()!=0){
					String[] synchunks = new String[2];
					synchunks=namestr.split("\\s");
					for(int x=0;x<synchunks.length;x++)
					{
						this.names.add(synchunks[x].trim().toString().toLowerCase());
					}
				}
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}		
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Connection conn = null;
		try{

			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
			    String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
			}
			String transformeddir = "C:\\Users\\mohankrishna89\\Desktop\\remarkup\\Plazi_8538_pyr_mad_tx1\\target\\transformed";
			String outputtablename = "plazi_8538_taxonnames";
			String volume = "plazi_8538";
			TaxonNameCollector tnc = new TaxonNameCollector4TaxonX(conn, transformeddir, outputtablename, volume);
			tnc.collect4TaxonX();
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			}finally{
				try{
					conn.close();
				}catch(Exception e){
					e.printStackTrace();
				}
			}


		

	}

}
