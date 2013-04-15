 /* $Id$ */
/**
 * 
 */
package fna.charactermarkup;

/**
 * @author hongcui
 *
 */
public class ChunkRatio extends Chunk {
	String name;
	/**
	 * @param text
	 */
	public ChunkRatio(String text) {
		super(text);

	}
	
	public void setName(String name){
		this.name = name;
	}


	public String getName() {

		return name;
	}
}
