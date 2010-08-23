/**
 * 
 */
package fna.parsing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import fna.parsing.character.Glossary;

/**
 * @author hongcui
 * Move the dyhypen() function from VolumeDehypenizer, to make DeHyphenAFolder a utility class that can be called by other projects.
 */
public class DeHyphenAFolder {
	private ProcessListener listener;
	private String database;
	private Text perlLog;
	private String dataPrefix;
	private String tablename;
	private Glossary glossary;
	private File folder;
	private File outfolder;
	private static final Logger LOGGER = Logger.getLogger(DeHyphenAFolder.class);
	private Connection conn;
    static public String num = "\\d[^a-z]+";
    private Hashtable<String,String> mapping = new Hashtable<String, String>();
	/**
	 * 
	 */
	public DeHyphenAFolder(ProcessListener listener, String workdir, 
    		String todofoldername, String database, Text perlLog, String dataPrefix, Glossary glossary) {
		this.listener = listener;
        this.database = database;
        this.perlLog = perlLog;
        this.dataPrefix = dataPrefix;
        this.tablename = dataPrefix+"_allwords";
        
        this.glossary = glossary;
        workdir = workdir.endsWith("/")? workdir : workdir+"/";
        this.folder = new File(workdir+todofoldername);
        this.outfolder = new File(workdir+ApplicationUtilities.getProperty("DEHYPHENED"));
        if(!outfolder.exists()){
            outfolder.mkdir();
        }
        
        try{
            if(conn == null){
                Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
                String URL = ApplicationUtilities.getProperty("database.url");
                conn = DriverManager.getConnection(URL);
                //createNumTextMixTable();
                createWordTable();
            }
        }catch(Exception e){
        	LOGGER.error("Database is down! (VolumeDehyphenizer)", e);
            e.printStackTrace();
        }
	}
	
	   public void dehyphen(){

		   if(listener!= null) listener.progress(1);
	        fillInWords();
	        if(listener!= null) listener.progress(50);

	        DeHyphenizer dh = new DeHyphenizerCorrected(this.database, this.tablename, "word", "count", "-", dataPrefix, glossary);

	        try{
	            Statement stmt = conn.createStatement();
	            ResultSet rs = stmt.executeQuery("select word from "+tablename+" where word like '%-%'");
	            while(rs.next()){
	                String word = rs.getString("word");
	                String dhword = dh.normalFormat(word);
	                //System.out.println(word+"===>"+dhword);
	                //MainForm.markUpPerlLog.append(word+"===>"+dhword+"\n");
	                mapping.put(word, dhword);
	            }
	        }catch(Exception e){
	        	LOGGER.error("Problem in VolumeDehyphenizer:dehyphen", e);
	            e.printStackTrace();
	        }
	        normalizeDocument();
	        if(listener!= null) listener.progress(100);
	    }
	    
	    private void createWordTable(){
	        try{
	            Statement stmt = conn.createStatement();
	            String query = "create table if not exists "+tablename+" (word varchar(150) unique not null primary key, count int)";
	            stmt.execute(query);
	            stmt.execute("delete from "+tablename);
	        }catch(Exception e){
	        	LOGGER.error("Problem in VolumeDehyphenizer:createWordTable", e);
	            e.printStackTrace();
	        }
	    }
	    
	    /*private void createNumTextMixTable(){
	        try{
	            Statement stmt = conn.createStatement();
	            String query = "create table if not exists "+tablename1+" (id int not null auto_increment primary key, mix varchar(30), file varchar(400))";
	            stmt.execute(query);
	            stmt.execute("delete from "+tablename1);
	        }catch(Exception e){
	            e.printStackTrace();
	        }
	    }*/
	    private void fillInWords(){
	        try {
	            Statement stmt = conn.createStatement();
	            File[] flist = folder.listFiles();
	            for(int i= 0; i < flist.length; i++){
	                //System.out.println("read "+flist[i].getName());
	                //MainForm.markUpPerlLog.append("read "+flist[i].getName()+"\n");
	                BufferedReader reader = new BufferedReader(new FileReader(flist[i]));
	                String line = null;
	                while ((line = reader.readLine()) != null) {
	                    line = line.toLowerCase();
	                    String linec = line;
	                    /*if(line.matches(".*?\\d+-(?=[a-z]).*")){
	                        line = fixNumTextMix(line, flist[i]);
	                    }*/
	                    line = line.replaceAll("<[^<]+?>", " "); //for xml or html docs
	                    line = line.replaceAll(num, " ");
	                    line = line.replaceAll("[^-a-z]", " ");

	                    line = normalize(line);
	                    
	                    //System.err.println("line has changed from \n"+linec+" to \n"+line);
	                
	                    String[] words = line.split("\\s+");
	                    for(int j = 0; j < words.length; j++){
	                        String w = words[j].trim();
	                        if(w.matches(".*?\\w.*")){
	                            int count = 1;
	                            ResultSet rs = stmt.executeQuery("select word, count from "+tablename+"  where word='"+w+"'");
	                            if(rs.next()){
	                                count = rs.getInt("count")+1;
	                            }
	                            stmt.execute("delete from "+tablename+" where word ='"+w+"'");
	                            stmt.execute("insert into "+tablename+" values('"+w+"', "+count+")");
	                        }
	                    }
	                }
	                reader.close();
	            }
	        } catch (Exception e) {
	        	LOGGER.error("Problem in VolumeDehyphenizer:fillInWords", e);
	            e.printStackTrace();
	        }
	    }
	    /**
	     * save original text mix in File source in a table,
	     * to be used in outputting final text
	     * @param mix
	     * @param source
	     * @return
	     */
	    /*private String fixNumTextMix(String mix, File source){
	        StringBuffer fixed = new StringBuffer();
	        Pattern p = Pattern.compile("(.*?)(\\d+-)([a-z].*)");
	        Matcher m = p.matcher(mix);
	        while(m.matches()){
	            fixed.append(m.group(1)).append("NUM-");
	            String save = m.group(2)+m.group(3);
	            save = save.substring(0, save.length() < mixlength ? save.length() : mixlength );
	            //save to table
	            mix = m.group(3);
	            try{
	                Statement stmt = conn.createStatement();
	                stmt.execute("insert into "+tablename1+" (mix, file) values ('"+save+"', '"+source.getName()+"')");
	            }catch (Exception e){
	                e.printStackTrace();
	            }
	        }
	        fixed.append(mix);
	        return fixed.toString();
	    }*/
	    
	    private String fixBrokenHyphens(String broken){ //cup-[,]  disc-[,]  or dish-shaped
	        StringBuffer fixed = new StringBuffer();
	        Pattern p = Pattern.compile("(.*?\\b)([a-z]+)-\\W[^\\.]*?[a-z]+-([a-z]+)(.*)");
	        Matcher m = p.matcher(broken);
	        while(m.matches()){
	            String begin = m.group(1);
	            String part = broken.substring(m.start(2), m.start(3));
	            broken = m.group(4);
	            String fix = m.group(3);
	            part = part.replaceAll("-(?!\\w)", "-"+fix);
	            fixed.append(begin+part);
	            m = p.matcher(broken);
	        }
	        fixed.append(broken);
	        return fixed.toString();
	    }
	    private void normalizeDocument(){
	        try {
	            File[] flist = folder.listFiles();
	            for(int i= 0; i < flist.length; i++){
	                BufferedReader reader = new BufferedReader(new FileReader(flist[i]));
	                String line = null; //DO NOT normalize case
	                StringBuffer sb = new StringBuffer();
	                while ((line = reader.readLine()) != null) {
	                    line = line.replaceAll(System.getProperty("line.separator"), " ");
	                    sb.append(line);
	                }
	                reader.close();
	                String text = sb.toString();
	                text = normalize(text);

	                text = performMapping(text);
	                //write back
	                //System.out.println(text);
	                File outf = new File(outfolder, flist[i].getName());
	                //BufferedWriter out = new BufferedWriter(new FileWriter(flist[i]));
	                BufferedWriter out = new BufferedWriter(new FileWriter(outf));
	                out.write(text);
	                out.close();
	                //System.out.println(flist[i].getName()+" dehyphenized");
	                //MainForm.markUpPerlLog.append(flist[i].getName()+" dehyphenized\n");
	            }
	        } catch (Exception e) {
	        	LOGGER.error("Problem in VolumeDehyphenizer:normalizeDocument", e);
	            e.printStackTrace();
	        }
	    }
	    
	    private String normalize(String text){
	        text = text.replaceAll("-+", "-");
	        
	        Pattern p = Pattern.compile("(.*?\\W)-(.*)"); //remove proceeding -
	        Matcher m = p.matcher(text);
	        while(m.matches()){
	            text = m.group(1)+" "+m.group(2); 
	            m = p.matcher(text);
	        }
	        
	        p = Pattern.compile("(.*?)-(\\W.*)"); //remove trailing
	        m = p.matcher(text);
	        while(m.matches()){
	            text = m.group(1)+" "+m.group(2);
	            m = p.matcher(text);
	        }
	        //text = text.replaceAll("\\W-", " "); 
	        //text = text.replaceAll("-\\W", " ");
	        //HOng, 08/04/09 for FoC doc. "-" added in place of <dox-tags>.
	        /*if(line.matches(".*?[a-z]- .*")){//cup-  disc-  or dish-shaped
	            line = fixBrokenHyphens(line); //Too loose. 
	        }*/
	        /*if(text.matches(".*?[a-z]-[^a-z0-9].*")){//cup-  disc-  or dish-shaped
	        text = fixBrokenHyphens(text);
	        }*/
	        return text;
	    }
	    
	    private String performMapping(String original){
	        Enumeration en = mapping.keys();
	        while(en.hasMoreElements()){
	            String hword = (String)en.nextElement();
	            String dhword = (String)mapping.get(hword);
	            //System.out.println("hword: "+hword +" dhword: "+dhword);
	            if(!hword.equals(dhword) && !hword.startsWith("-") && !hword.endsWith("-")){
	                //replace those in lower cases
	                original = original.replaceAll(hword, dhword);
	                //hyphen those phrases that are hyphened once 
	                String dhw = dhword.replaceAll("-", " "); //cup-shaped => cup shaped
	                original = original.replaceAll(dhw, dhword); //cup shaped =>cup-shaped
	                //upper cases
	                hword = hword.toUpperCase().substring(0,1)+hword.substring(1);
	                dhword = dhword.toUpperCase().substring(0,1)+dhword.substring(1);
	                original = original.replaceAll(hword, dhword);
	                dhw = dhword.replaceAll("-", " "); //Cup-shaped => Cup shaped
	                original = original.replaceAll(dhw, dhword); //Cup shaped =>Cup-shaped
	            }
	        }
	        return original;
	    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DeHyphenAFolder dhaf = new DeHyphenAFolder(null, "X:\\DATA\\Plazi\\2ndFetchFromPlazi\\target-taxonX-fish", 
		"descriptions", "markedupdatasets", null, "plazi_fish_clause", null);
		dhaf.dehyphen();

	}

}
