package fna.parsing.state;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;

import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import edu.uci.ics.jung.visualization.DefaultVisualizationModel;
import edu.uci.ics.jung.visualization.VisualizationModel;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.VertexLabelAsShapeRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel.Position;
import edu.uci.ics.jung.algorithms.cluster.*;
import edu.uci.ics.jung.algorithms.filters.EdgePredicateFilter;
import edu.uci.ics.jung.algorithms.filters.VertexPredicateFilter;
import edu.uci.ics.jung.algorithms.layout.*;

import java.util.*;

import javax.swing.JFrame;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;



public class StateMatrix {
	
	private ArrayList<State> states = null;
	private ArrayList<Cell> matrix = null;
	private int edgeCount = 0;
	//private Hashtable<State, Hashtable<State, CoocurrenceScore>> matrix = null;
	
	StateMatrix(){
		states = new ArrayList<State>();
		matrix = new ArrayList<Cell>();
		//matrix = new Hashtable<State, Hashtable<State, CoocurrenceScore>>();
	}

	/**
	 * empty matrix with the knownstates as the row and column
	 * @param knownstates
	 */
	StateMatrix(Set<State> knownstates){
		//matrix = new Hashtable<State, Hashtable<State, CoocurrenceScore>>();
		Iterator<State> it = knownstates.iterator();
		//Hashtable<State, CoocurrenceScore> row = new Hashtable<State, CoocurrenceScore>();
		while(it.hasNext()){
			State s = (State)it.next();
			states.add(s);
			//row.put(s, null);
		}
		/*it = knownstates.iterator();
		while(it.hasNext()){
			matrix.put((State)it.next(), row);
		}*/
	}
	
	private ArrayList<State> getStates(){
		//return matrix.keySet();
		return states;
	}
	
	/**
	 * 
	 * @param s1
	 * @param s2
	 * @param score: count = 1 for now
	 */
	public void addPair(State s1, State s2, int score, String source){
		//which one(s) is a new state?
		if(s1.isEmpty() || s2.isEmpty()){
			return;
		}
		
		int f1 = 0;
		int f2 = 0;
		if(states.contains(s1)){
			f1 = 1;
		}
		if(states.contains(s2)){
			f2 = 1;
		}
		
		if(f1+f2 == 0){//two new states
			states.add(s1);
			states.add(s2);
			int i1 = states.indexOf(s1);
			int i2 = states.indexOf(s2);
			Cell s1s2 = new Cell(i1, i2, new CoocurrenceScore(score, source));
			Cell s2s1 = new Cell(i2, i1, new CoocurrenceScore(score, source));
			matrix.add(s1s2);
			matrix.add(s2s1);
		}
		
		if(f1+f2 == 1){//one new state
			State newstate = f1 == 0? s1: s2;
			State existstate = newstate == s1? s2: s1;
			states.add(newstate);
			int i1 = states.indexOf(existstate);
			int i2 = states.indexOf(newstate);
			Cell en = new Cell(i1, i2, new CoocurrenceScore(score, source));
			Cell ne = new Cell(i2, i1, new CoocurrenceScore(score, source));
			matrix.add(en);
			matrix.add(ne);
		}
		
		if(f1+f2 == 2){// 0 new state, update the score
			int i1 = states.indexOf(s1);
			int i2 = states.indexOf(s2);
			Cell c = new Cell(i1, i2, null);
			int cellindex = matrix.indexOf(c);
			if(cellindex >=0){
				Cell e = matrix.get(cellindex);
				CoocurrenceScore cs = e.getScore();
				cs.updateBy(score, source);
			}else{
				matrix.add(new Cell(i1, i2, new CoocurrenceScore(score, source)));
			}
			
			c = new Cell(i2, i1, null);
			cellindex = matrix.indexOf(c);
			if(cellindex >=0){
				Cell e = matrix.get(cellindex);
				CoocurrenceScore cs = e.getScore();
				cs.updateBy(score, source);
			}else{
				matrix.add(new Cell(i2, i1, new CoocurrenceScore(score, source)));
			}
		}
		
	}
	
	public State getStateByName(String state){
		Iterator<State> en = states.iterator();
		while(en.hasNext()){
			State s = en.next();
			if(s.getName().compareTo(state)==0){
				return s;
			}
		}
		return null;
	}
	
	public void save2MySQL(String database, String username, String password){
		Connection conn = null;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists terms (term varchar(100), cooccurTerm varchar(100), frequency int(4), sourceFiles varchar(2000), primary key(term, cooccurTerm))");
				stmt.execute("delete from terms");
			}
		
		
			Collections.sort(matrix, new Cell());
			StringBuffer sb = new StringBuffer("");
			Cell c = null;
			int n = states.size();
			for(int i = 0; i < n; i++){
				for(int j = 0; j < n; j++){
					c = new Cell(i, j, null);
					if(matrix.contains(c)){
						State s1 = states.get(i);
						State s2 = states.get(j);
						//Statement stmt = conn.createStatement();
						//ResultSet rs = stmt.executeQuery("select term from terms where term = '"+s1.getName()+"' and cooccurTerm='"+s2.getName()+"'");
						Cell data = matrix.get(matrix.indexOf(c));
						CoocurrenceScore score = data.getScore();//"[]" is an empty score
						String values = "'"+s1.getName()+"','";

						String src = score.getSourcesAsString();
						src = src.length() >=2000? src.substring(0, 1999) : src;
						values +=s2.getName()+"',"+score.getSources().size()+",'"+src+"'";

						Statement stmt = conn.createStatement();
						stmt.execute("insert into terms values("+values+")");
					}
				}
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	/**
	 * group cooccured nodes into groups, using JUNG libary.
	 * 
	 */
	public void Grouping(){
		//construct the graph
		Graph<State, MyLink> g = new UndirectedSparseMultigraph<State, MyLink>();
		Iterator<Cell> it = this.matrix.iterator();
		while(it.hasNext()){
			Cell c = it.next();
			State node1  = this.states.get(c.getCindex());
			State node2 = this.states.get(c.getRindex());
			int weight = c.getScore().valueSum();
			if(weight>1){
				System.out.println(weight+" links "+node1.getName()+" and "+node2.getName());
			}
			g.addVertex(node1);
			g.addVertex(node2);
			for(int i = 0; i<weight; i++){
				g.addEdge(new MyLink(1, edgeCount++), node1, node2);
			}
			
		}
		/*
		//visualize the graph
		EdgePredicateFilter<State, MyLink> f1 = new EdgePredicateFilter<State, MyLink>(new LinkPredicate());
		VertexPredicateFilter<State, MyLink> f2 = new VertexPredicateFilter<State, MyLink>(new VertexPredicate(g));
		g = f1.transform(g);
		g = f2.transform(g);
		//Layout<State, MyLink> layout = new KKLayout<State, MyLink>(g); //a big round circle with vertex on top of each other
		//Layout<State, MyLink> layout = new FRLayout<State, MyLink>(g);//recognizable groups in the center
		Layout<State, MyLink> layout = new CircleLayout<State, MyLink>(g);


		final VisualizationModel<State,MyLink> visualizationModel = 
		            new DefaultVisualizationModel<State,MyLink>(layout, new Dimension(1400,800));
		VisualizationViewer vv =  new VisualizationViewer<State, MyLink>(visualizationModel, new Dimension(1400,800));

		VertexLabelAsShapeRenderer<State,MyLink> vlasr = new VertexLabelAsShapeRenderer<State,MyLink>(vv.getRenderContext());
		Transformer<MyLink, Stroke> edgeStrokeTransformer =
					new Transformer<MyLink, Stroke>() {
						public Stroke transform(MyLink l) {
							Stroke edgeStroke = new BasicStroke(1 + (l.getWeight()/10.0f), BasicStroke.CAP_BUTT,
									BasicStroke.JOIN_MITER);
							return edgeStroke;
						}
				};

		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		vv.getRenderContext().setVertexShapeTransformer(vlasr);
		vv.getRenderContext().setVertexLabelRenderer(new DefaultVertexLabelRenderer(Color.red));
		vv.getRenderContext().setEdgeDrawPaintTransformer(new ConstantTransformer(Color.yellow));
		vv.getRenderContext().setEdgeStrokeTransformer(edgeStrokeTransformer);
		vv.getRenderer().setVertexLabelRenderer(vlasr);
		vv.setBackground(Color.black);
		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
		JFrame frame = new JFrame("Simple Graph View 2");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(vv);
		frame.pack();
		frame.setVisible(true);
		 */
		
		//use different algorithms to group the graph
		/*
		//1. Bicomponent Clustering: 336 groups.
		System.out.println("Bicomponent Clustering");
		BicomponentClusterer bc = new BicomponentClusterer();
		Set groups = (Set)bc.transform(g);
		printGroups(groups);
		
		//2. Edge Betweenness Clustering: 31 groups, 1 large group, some states may not have been included (total states 698)
		System.out.println("Edge Betweenness Clustering");
		EdgeBetweennessClusterer ebc = new EdgeBetweennessClusterer(1); //use any number for 1 here
		groups = ebc.transform(g);
		printGroups(groups);
		
		//3. Week Component Clustering, 31 groups similiar to the above
		System.out.println("Week Component Clustering");
		WeakComponentClusterer wcc = new WeakComponentClusterer();
		groups = wcc.transform(g);
		printGroups(groups);
		*/
		//4. Voltage Clustering: 21  groups of varied sizes
		System.out.println("Voltage Clustering");
		VoltageClusterer vc = new VoltageClusterer(g, 50);
		Collection clusters = vc.cluster(50);
		printGroups(clusters);
	}

	private void printGroups(Collection groups) {
		Iterator<Set> sets = groups.iterator();
		int gcount = 0;
		while(sets.hasNext()){
			Set states = (Set)sets.next();
			System.out.println("Group "+gcount+ ":");
			Iterator<Set> sit = states.iterator();
			while(sit.hasNext()){
				State s = (State)sit.next();
				System.out.print("state "+s.getName()+ "\t");
			}
			System.out.println(" ");
			gcount++;
		}
	}
	private void printGroups(Set groups) {
		Iterator<Set> sets = groups.iterator();
		int gcount = 0;
		while(sets.hasNext()){
			Set states = (Set)sets.next();
			System.out.println("Group "+gcount+ ":");
			Iterator<Set> sit = states.iterator();
			while(sit.hasNext()){
				State s = (State)sit.next();
				System.out.print("state "+s.getName()+ "\t");
			}
			System.out.println(" ");
			gcount++;
		}
	}
	
	
	
	public String toString(){
		Collections.sort(matrix, new Cell());
		StringBuffer sb = new StringBuffer("");
		Cell c = null;
		int n = states.size();
		for(int i = 0; i < n; i++){
			for(int j = 0; j < n; j++){
				c = new Cell(i, j, null);
				if(matrix.contains(c)){
					State s1 = states.get(i);
					State s2 = states.get(j);
					Cell data = matrix.get(matrix.indexOf(c));
					String score = data.getScore().toString();//"[]" is an empty score
					//if(score.length() > 2){
						sb.append(s1.toString()+" coocurred with: ");
						sb.append("\t"+s2.toString()+" "+score+"\n");
					//}
				}
			}
		}
		return sb.toString();
	}
					 
}
