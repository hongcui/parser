package testing_lab;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class Test {

	/**
	 * @param args
	 */
	public String toString() {
		return "Hola!";
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		//String a = "TabItem {Segmentation}";
		//System.out.println(a.substring(a.indexOf("{")+1, a.indexOf("}")));
		//String ab = "hhhhhh=ooooooo";
		//System.out.println(ab.substring(0, ab.indexOf("=")));
		//System.out.println(ab.substring(ab.indexOf("=")+1));
		
		/*HashMap hm = new HashMap();
		hm.put("1", "Partha");
		System.out.println(hm.get("1"));
		System.out.println(hm.get("1"));*/
/*		ArrayList alist = new ArrayList();
		alist.add(new HashMap());
		alist.add(new String("p"));
		alist.add(new Test());
		
		System.out.println(alist.get(0));
		System.out.println(alist.get(1));
		System.out.println(alist.get(2));*/
		String a = "adsgsbfsbdrrr35gtebHeadking1cccc";
		String b = "$*YYYHeading2ssss";
		String c = "Heading3" ;
		String d = "Hearding4" ;
		String e = "345He0ading5" ;
		String [] aa = {a,b,c,d,e};
		String common = longestCommonSubstring(aa[0], aa[1]);
		for (int i = 2; i < 5; i++) {
			common = longestCommonSubstring(common, aa[i]);
		}
		System.out.println(common);
	}
	
/*	public static String longestCommonSubstring(String str1, String str2)
	{
		String sequence = "";
		if (str1 == null || str1.equals("") || str2 == null || str2.equals("")) {
			return "";
		}
			
	 
		int[][]num = new int[str1.length()][str2.length()];
		int maxlen = 0;
		int lastSubsBegin = 0;
		StringBuilder sequenceBuilder = new StringBuilder();
	 
		for (int i = 0; i < str1.length(); i++)
		{
			for (int j = 0; j < str2.length(); j++)
			{
				if (str1.charAt(i) != str2.charAt(j))
					num [i][j] = 0;
				else
				{
					if ((i == 0) || (j == 0))
						num[i][j] = 1;
					else
						num[i][j] = 1 + num[i - 1][j - 1];
	 
					if (num[i][j] > maxlen)
					{
						maxlen = num[i][j];
						int thisSubsBegin = i - num[i][j] + 1;
						if (lastSubsBegin == thisSubsBegin)
						{//if the current LCS is the same as the last time this block ran
							sequenceBuilder.append(str1.charAt(i));
						}
						else //this block resets the string builder if a different LCS is found
						{
							lastSubsBegin = thisSubsBegin;
							sequenceBuilder.delete(0, sequenceBuilder.length());//clear it
							sequenceBuilder.append(str1.substring(lastSubsBegin, (i + 1) - lastSubsBegin));
						}
					}
				}
			}
		}
		sequence = sequenceBuilder.toString();
		return sequence;
	}*/

/*	public int longestSubstr(String str_, String toCompare_) 
	{
		StringBuilder sequence = new StringBuilder();
	  if (str_.isEmpty() || toCompare_.isEmpty())
	    return 0;
	 
	  int[][] compareTable = new int[str_.length()][toCompare_.length()];
	  int maxLen = 0;
		int lastSubsBegin = 0;
	 
	  for (int m = 0; m < str_.length(); m++) 
	  {
	    for (int n = 0; n < toCompare_.length(); n++) 
	    {
	      compareTable[m][n] = (str_.charAt(m) != toCompare_.charAt(n)) ? 0
	          : (((m == 0) || (n == 0)) ? 1
	              : compareTable[m - 1][n - 1] + 1);
	      maxLen = (compareTable[m][n] > maxLen) ? compareTable[m][n]
	          : maxLen;
	    }
	  }
	  return maxLen;
	}*/
	
	public static String longestCommonSubstring(String first, String second) {
		 
		 String tmp = "";
		 String max = "";
						
		 for (int i=0; i < first.length(); i++){
			 for (int j=0; j < second.length(); j++){
				 for (int k=1; (k+i) <= first.length() && (k+j) <= second.length(); k++){
										 
					 if (first.substring(i, k+i).equals(second.substring(j, k+j))){
						 tmp = first.substring(i,k+i);
					 }
					 else{
						 if (tmp.length() > max.length())
							 max=tmp;
						 tmp="";
					 }
				 }
					 if (tmp.length() > max.length())
							 max=tmp;
					 tmp="";
			 }
		 }
				
		 return max;        
			    
	}



}
