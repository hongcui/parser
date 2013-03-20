/**
 * 
 */
package biosemantics.ontologies;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import fna.parsing.state.SentenceOrganStateMarker;

/**
 * @author hong cui
 * This class fetches selected classes from an OWL ontology and put them in a relational database table
 *
 */
public abstract class OntologyClassFetcher {
	protected static final Logger LOGGER = Logger.getLogger(SentenceOrganStateMarker.class);
	protected Set<OWLClass> allclasses=new HashSet<OWLClass>();
	protected Set<OWLOntology> onts=new HashSet<OWLOntology>();
	protected OWLOntology ontology;
	protected OWLDataFactory df;
	protected Connection conn;
	protected String table;
	//the following three arraylists are synchronised on their indexes: 1 class will have id, label, and categories
	protected ArrayList<String> selectedClassLabels = new ArrayList<String>();
	protected ArrayList<String> selectedClassCategories = new ArrayList<String>();
	protected ArrayList<String> selectedClassIds = new ArrayList<String>();
	/**
	 * table: must have at least three fields: ontoid, term, category, 
	 */
	public OntologyClassFetcher(String ontopath, Connection conn, String table) {
		this.table = table;
		this.conn = conn;
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		df = manager.getOWLDataFactory();
		File ontofile = new File(ontopath);
		//IRI iri = IRI.create(ontoURL);

		try {
			//fetch all classes
			ontology = manager.loadOntologyFromOntologyDocument(ontofile);
			onts = ontology.getImportsClosure();
			for (OWLOntology ont:onts){
				allclasses.addAll((Collection<? extends OWLClass>) ont.getClassesInSignature(true));
			}	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * populate the arraylists selectedClassLabels and selectedClassCategories
	 */
	public abstract void selectClasses();
	
	public ArrayList<String> getSynonymLabels(OWLClass c) {
		ArrayList<String> labels = new ArrayList<String>();
		Set<OWLAnnotation> anns = c.getAnnotations(ontology, df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasExactSynonym")));
		anns.addAll(c.getAnnotations(ontology, df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym"))));
		anns.addAll(c.getAnnotations(ontology, df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasBroadSynonym"))));
		anns.addAll(c.getAnnotations(ontology, df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#hasNarrowSynonym"))));
		
		Iterator<OWLAnnotation> it = anns.iterator();
		while (it.hasNext()) {
			//String label = this.getRefinedOutput(it.next().toString());
			String label = ((OWLLiteral)it.next().getValue()).getLiteral();
			labels.add(label);
		}
		return labels;
	}
	
	public void saveSelectedClass(){
		try{
			//table: must have at least three fields: ontoid, term, category, 
			PreparedStatement stmt = conn.prepareStatement("insert into "+table+"(ontoid, term, category) values (?, ?, ?)");
			for(int i = 0; i < this.selectedClassIds.size(); i++){
				stmt.setString(1, this.selectedClassIds.get(i));
				stmt.setString(2, this.selectedClassLabels.get(i));
				stmt.setString(3, this.selectedClassCategories.get(i));
				stmt.execute();
			}
			
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
