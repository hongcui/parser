/**
 * 
 */
package fna.charactermarkup;

/**
 * @author Hong Updates
 *
 */
import java.util.*;
import java.io.*;
class StreamGobbler extends Thread
{
    InputStream is;
    String type;
    OutputStream os;
    ArrayList<String> headings = new ArrayList<String>();
    ArrayList<String> trees = new ArrayList<String>();
    static int h = 0;
    static int t = 0;
    StreamGobbler(InputStream is, String type, ArrayList<String> headings, ArrayList<String> trees)
    {
        this.is = is;
        this.type = type;
        this.headings = headings;
        this.trees = trees;
    }
    
    public void run()
    {
        try
        {  	
        	PrintWriter pw = null;
            if (os != null)
                pw = new PrintWriter(os);
                
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            StringBuffer sb = new StringBuffer();

            while ( (line = br.readLine()) != null)
            {	
            	System.out.println(type+">"+line);
            	if(line.startsWith("Parsing [sent.")){
            		headings.add(line);
            		System.out.println(h+" add heading: "+line);
            		h++;
            	}else{
            		if(line.startsWith("(ROOT") || line.startsWith("SENTENCE_SKIPPED_OR_UNPARSABLE")){
            			if(sb.toString().trim().length()>0){
            				trees.add(sb.toString());
            				System.out.println(t+" add tree: "+sb.toString());
            				t++;
            			}
            			sb = new StringBuffer();
            			sb.append(line+System.getProperty("line.separator"));
            		}else if(line.matches("^\\s*\\(.*")){
            			sb.append(line+System.getProperty("line.separator"));
            		}
            	}
            }
            if(sb.toString().trim().length()>0){
            	trees.add(sb.toString());
            }
        } catch (IOException ioe)
            {
            ioe.printStackTrace();  
            }
    }
}

