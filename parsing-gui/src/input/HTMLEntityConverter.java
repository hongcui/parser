/**
 * 
 */
package input;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * @author Hong Cui
 *Convert HTML entities to corresponding characters in UTF-8
 */
public class HTMLEntityConverter {

	/**
	 * 
	 */
	public HTMLEntityConverter() {
		
	}

	public static String convert2UTF8(String sentence){
		return StringEscapeUtils.unescapeHtml4(sentence);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println((new HTMLEntityConverter()).convert2UTF8("leaves &#217;-shaped"));

	}

}
