/**
 * 
 */
package fna.parsing.state;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import fna.parsing.ApplicationUtilities;
import fna.parsing.ParsingException;
import fna.parsing.Registry;

/**
 * @author Hong Updates
 *
 */
public class GraphMLOutputter {
	private Iterator<Set> sets = null;
	private StateMatrix matrix = null;
	private static String nl = System.getProperty("line.separator");
	public static String header = "<?xml version='1.0' encoding='UTF-8' standalone='no'?> " +nl+
			"<graphml xmlns='http://graphml.graphdrawing.org/xmlns' " +nl+
			"xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +nl+
			"xsi:schemaLocation='http://graphml.graphdrawing.org/xmlns " +nl+
			"http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd'> " +nl+
			"<graph edgedefault='undirected' id='graph'> " +nl+
			"<key attr.name='name' attr.type='string' for='node' id='name'/> " +nl+
			"<key attr.name='type' attr.type='string' for='all' id='type'/> " +nl+
			"<key attr.name='weight' attr.type='float' for='edge' id='weight'/>"+nl;
	/**
	 * 
	 */
	public GraphMLOutputter(Object nodegroups, StateMatrix matrix) {
		
		if(nodegroups instanceof Collection){
			this.sets = ((Collection)nodegroups).iterator();
		}else if(nodegroups instanceof Set){
			this.sets = ((Set)nodegroups).iterator();
		}
		this.matrix = matrix;
		
	}
	/**
	 * output groups as a GraphML document, with nodes and links
	 * @param sets
	 */

	public void output(){		
		int gcount = 1;
		
		while(sets.hasNext()){
			String graphXML = GraphMLOutputter.header+nl;
			Set states = (Set)sets.next();
			System.out.println("Group "+gcount+ ":");
			Iterator<State> sit = states.iterator();
			Hashtable<String, String> nodes = new Hashtable<String, String>();
			//output nodes
			int nid = 1;
			//<node id='3'>
			//<data key='name'>stem</data>
			//</node>
			while(sit.hasNext()){
				State s = sit.next();
				String name = s.getName();
				graphXML += "<node id='"+nid+"'><data key='name'>"+name+"</data></node>"+nl;
				nodes.put(nid+"", name);
				nid++;
				//System.out.print("state "+s.getName()+ "\t");
			}
			
			//output edges
			int edgecount = 1;
			int totalv = 0;
			for(int i = 1; i < nid; i++){
				for(int j = i+1; j < nid; j++){
					String str1 = nodes.get(i+"");
					String str2 = nodes.get(j+"");
					CooccurrenceScore score = matrix.cooccurScore(str1, str2);
					if(score!=null){
						int v = score.valueSum();
						if(v!=0){
							//<edge source='4' target='3'/>
							totalv += v;
							graphXML +="<edge source='"+i+"' target='"+j+"' weight='"+normalize(v, totalv, edgecount)+"'/>"+nl;
							edgecount++;
						}
					}
				}
			}
			graphXML+="</graph>"+nl+"</graphml>";
			output2file(gcount, graphXML);
			gcount++;
		}
	}
		
	private float normalize(int v, int total, int count) {
		// TODO Auto-generated method stub
		return 1;
	}
	
	private void output2file(int id, String text) {
		try {
			String path = Registry.TargetDirectory;
			File file = new File(path, ApplicationUtilities.getProperty("CHARACTER-STATES") + "/" +"Group_"+id+".xml");
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(text);
			out.close(); // don't forget to close the output stream!!!
		} catch (IOException e) {
			e.printStackTrace();
			//LOGGER.error("", e);
			//throw new ParsingException("", e);
		}		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
