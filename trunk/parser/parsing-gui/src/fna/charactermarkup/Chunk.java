/**
 * 
 */
package fna.charactermarkup;

/**
 * @author hongcui
 *
 */
public class Chunk {
	protected String text = null;
	public Chunk(String text){
		this.text = text;	
	}
	
	protected String getText(){
		return this.text;
	}
}
