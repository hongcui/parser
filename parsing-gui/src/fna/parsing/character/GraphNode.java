package fna.parsing.character;

import java.util.ArrayList;

public class GraphNode {

	private String nodeName;
	private String nodeNumber;
	private ArrayList<String> edges;
	
	public GraphNode (String nodeName) {
		this.nodeName = nodeName;
		edges = new ArrayList<String>();
	}

	private GraphNode(){
		
	}
	/**
	 * @return the nodeName
	 */
	public String getNodeName() {
		return nodeName;
	}
	/**
	 * @param nodeName the nodeName to set
	 */
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	/**
	 * @return the edges
	 */
	public ArrayList<String> getEdges() {
		return edges;
	}
	/**
	 * @param edges the edges to set
	 */
	public void setEdges(ArrayList<String> edges) {
		this.edges = edges;
	}

	/**
	 * @return the nodeNumber
	 */
	public String getNodeNumber() {
		return nodeNumber;
	}

	/**
	 * @param nodeNumber the nodeNumber to set
	 */
	public void setNodeNumber(String nodeNumber) {
		this.nodeNumber = nodeNumber;
	}
}
