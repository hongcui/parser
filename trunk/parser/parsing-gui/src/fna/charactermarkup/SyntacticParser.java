 /* $Id$ */
/**
 * 
 */
package fna.charactermarkup;

/**
 * @author hongcui
 *
 */
public interface SyntacticParser {


	public void POSTagging() throws Exception;
	public void parsing() throws Exception;
	public void extracting() throws Exception;

}
