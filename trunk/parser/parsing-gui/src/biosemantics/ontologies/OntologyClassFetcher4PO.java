/**
 * 
 */
package biosemantics.ontologies;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;

import fna.parsing.ApplicationUtilities;


/**
 * @author updates
 *
 */
public class OntologyClassFetcher4PO extends OntologyClassFetcher {
	private boolean debug = true;
	/**
	 * @param ontoURL
	 * @param conn
	 * @param table
	 */
	public OntologyClassFetcher4PO(String ontoURL, Connection conn, String table) {
		super(ontoURL, conn, table);
		
		try{
			//create table if not exist
			Statement stmt = conn.createStatement();
			stmt.execute("drop table if exists "+table);
			stmt.execute("create table if not exists "+table+"(ontoid varchar(50) NOT NULL Primary Key, term varchar(100), category varchar(100), head_noun varchar(50), remark text)");		
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/* (non-Javadoc)
	 * @see biosemantics.ontologies.OntologyClassFetcher#selectClasses()
	 * select anatomy classes from allclasses and populate selectedClass arraylists.
	 * <oboInOwl:hasOBONamespace rdf:datatype="http://www.w3.org/2001/XMLSchema#string">plant_anatomy</oboInOwl:hasOBONamespace>
	 */
	@Override
	public void selectClasses() {
		for(OWLClass aclass: allclasses){
			Set<OWLAnnotation> annotations = aclass.getAnnotations(ontology,
					df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasOBONamespace")));
			for(OWLAnnotation annotation: annotations){
				if(annotation.toString().contains("plant_anatomy")){
					//get and save class id
					String classid = aclass.getIRI().toString().replaceAll(".*?(?=PO_)", "");
					super.selectedClassIds.add(classid);
					if(debug) System.out.println(classid);
					//get and save the label for the class
					for (OWLAnnotation labelannotation : aclass
							.getAnnotations(ontology, df.getRDFSLabel())) {
							if (labelannotation.getValue() instanceof OWLLiteral) {
								OWLLiteral val = (OWLLiteral) labelannotation.getValue();
								super.selectedClassLabels.add(val.getLiteral());
							}
					}
					//save category
					super.selectedClassCategories.add("structure");
					//gather all types of synonyms (except synonyms in other languages)
					ArrayList<String> synonyms = getSynonymLabels(aclass);
					int count = 1;
					for(String synonym : synonyms){
						if(!synonym.matches(".*?\\([A-Z]\\w+\\)$")){ //(Japanese)
							super.selectedClassIds.add(classid+"_"+count++);
							super.selectedClassLabels.add(synonym);
							super.selectedClassCategories.add("structure");
						}
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
		String ontoURL = "C:/Users/updates/CharaParserTest/Ontologies/plant_ontology.owl";
		Connection conn = null;
		String table = "ontology_po_structures";
		String database=ApplicationUtilities.getProperty("database.name");
		String username=ApplicationUtilities.getProperty("database.username");
		String password=ApplicationUtilities.getProperty("database.password");
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
		OntologyClassFetcher4PO ocf4po = new OntologyClassFetcher4PO(ontoURL, conn, table);
		ocf4po.selectClasses();
		ocf4po.saveSelectedClass();
		ocf4po.recordHeadNouns();
	}


}
