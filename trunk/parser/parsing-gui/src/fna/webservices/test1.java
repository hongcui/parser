package fna.webservices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class test1 {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
/*       Scanner s = new Scanner("In addition to the species treated here");
       while (s.hasNext())
       System.out.println(s.next());*/
		HashMap <String, String> hm  = new HashMap<String, String>();
		
		hm.put("1", "dhcb");
		
		HashMap <String, String> hm1  = new HashMap<String, String>();
		hm.put("2", "dhcb");
		//x.charAt(1);
		
		hm.putAll(hm1);
		
		System.out.println(hm);
		
		
	}

}
