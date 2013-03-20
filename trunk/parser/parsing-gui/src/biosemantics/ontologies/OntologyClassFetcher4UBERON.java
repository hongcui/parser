/**
 * 
 */
package biosemantics.ontologies;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * @author updates
 *
 */
public class OntologyClassFetcher4UBERON extends OntologyClassFetcher {
	private boolean debug = true;
	/**
	 * @param ontopath
	 * @param conn
	 * @param table
	 */
	public OntologyClassFetcher4UBERON(String ontopath, Connection conn,
			String table) {
		super(ontopath, conn, table);
		try{
			//create table if not exist
			Statement stmt = conn.createStatement();
			stmt.execute("drop table if exists "+table);
			stmt.execute("create table if not exists "+table+"(id MEDIUMINT NOT NULL AUTO_INCREMENT Primary Key, ontoid varchar(200), term varchar(200), category varchar(100), head_noun varchar(50), remark text)");		
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see biosemantics.ontologies.OntologyClassFetcher#selectClasses()
	 */
	@Override
	public void selectClasses() {
		for(OWLClass aclass: allclasses){
			for(OWLOntology onto : super.onts){//each onto in the closure
				//get and save class id
				String classid = aclass.getIRI().toString().replaceAll(".*?(?=[A-Z]\\S+_[\\d]{7,7})", "");
				
				if(debug) System.out.println(classid);
				//get and save the label for the class
				String label = "";
				for (OWLAnnotation labelannotation : aclass
						.getAnnotations(onto, df.getRDFSLabel())) {
						if (labelannotation.getValue() instanceof OWLLiteral) {
							OWLLiteral val = (OWLLiteral) labelannotation.getValue();
							label = val.getLiteral();
							break;
						}
				}
				if(label.length()>0){
					super.selectedClassIds.add(classid);
					super.selectedClassLabels.add(label);
					//save category
					super.selectedClassCategories.add("structure");
				}
				
				//gather all types of synonyms (except synonyms in other languages)
				ArrayList<String> synonyms = getSynonymLabels(aclass);
				for(String synonym : synonyms){
					if(!synonym.matches(".*?\\([A-Z]\\w+\\)$")){ //(Japanese)
						super.selectedClassIds.add(classid+"_syn");
						super.selectedClassLabels.add(synonym);
						super.selectedClassCategories.add("structure");
					}
				}
				//get relational adjective
				Set<OWLAnnotation> annotations = aclass.getAnnotations(onto);
				for(OWLAnnotation anno : annotations){
					//System.out.println(anno.toString());
					//adjectiveorgans//adj => classID#label
					if(anno.toString().contains("UBPROP_0000007") ){//has_relational_adjective
						String adj = anno.getValue().toString();//"zeugopodial"^^xsd:string
						adj = adj.substring(0, adj.indexOf("^^")).replace("\"", "");
						super.selectedClassIds.add(classid+"_adj");
						super.selectedClassLabels.add(adj);
						super.selectedClassCategories.add("structure");
					}
				}
			}
		}
	}

	private void recordHeadNouns() {
		Statement stmt = null;
		PreparedStatement pstmt = null;
		try{
			stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select ontoid, term from "+table);
			while(rs.next()){
				String id = rs.getString("ontoid");
				String term = rs.getString("term");
				String headnoun = term.indexOf(" ") > 0? term.substring(term.lastIndexOf(" ")).trim() : term;
				pstmt = conn.prepareStatement("update "+table+" set head_noun= ? where ontoid= ? ");
				pstmt.setString(1, headnoun);
				pstmt.setString(2,  id);
				pstmt.executeUpdate();
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try{
				if(stmt != null) stmt.close();
				if(pstmt != null) pstmt.close();
			}catch(Exception e1){
				e1.printStackTrace();
			}
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String ontoURL = "C:/Users/updates/CharaParserTest/Ontologies/ext.owl";
		Connection conn = null;
		String table = "uberon_structures";
		String database="biocreative2012";
		String username="biocreative";
		String password="biocreative";
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password+"&connectTimeout=0&socketTimeout=0&autoReconnect=true";
				conn = DriverManager.getConnection(URL);
			}
		}catch(Exception e){
			e.printStackTrace();
			//StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
		}
		OntologyClassFetcher4UBERON ocf = new OntologyClassFetcher4UBERON(ontoURL, conn, table);
		ocf.selectClasses();
		ocf.saveSelectedClass();
		ocf.recordHeadNouns();
	}

}
