package fna.charactermarkup;

public class Relation {
	private String relationname;
	private String entity1;
	private String entity2;
	private boolean negation;
	
	public Relation(){
		
	}
	
	public Relation(String relationname, String entity1, String entity2, boolean negation){
		this.relationname = relationname;
		this.entity1 = entity1;
		this.entity2 = entity2;
		this.negation = negation;
	}

}
