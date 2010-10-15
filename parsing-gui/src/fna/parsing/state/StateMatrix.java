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
	private Connection conn = null;
	private String tableprefix = null;
	//private Hashtable<State, Hashtable<State, CoocurrenceScore>> matrix = null;
	
	StateMatrix(Connection conn, String tableprefix){
		states = new ArrayList<State>();
		matrix = new ArrayList<Cell>();
		this.tableprefix = tableprefix;
		this.conn = conn;
		Statement stmt = null;
		try{
			stmt = conn.createStatement();
			//stmt.execute("create table if not exists "+tableprefix+"_terms (term varchar(100), cooccurTerm varchar(100), frequency int(4), keep varchar(20), sourceFiles varchar(2000), primary key(term, cooccurTerm))");
			stmt.execute("create table if not exists "+tableprefix+"_terms (term varchar(100), cooccurTerm varchar(100), frequency int(4), keep varchar(20), sourceFiles varchar(2000))");
			stmt.execute("delete from "+tableprefix+"_terms");
			//stmt.execute("create table if not exists "+tableprefix+"_grouped_terms (groupId int, term varchar(100), cooccurTerm varchar(100), frequency int(4), keep varchar(20), sourceFiles varchar(2000), primary key(term, cooccurTerm))");
			stmt.execute("create table if not exists "+tableprefix+"_grouped_terms (groupId int, term varchar(100), cooccurTerm varchar(100), frequency int(4), keep varchar(20), sourceFiles varchar(2000))");
			stmt.execute("delete from "+tableprefix+"_grouped_terms");
			stmt.execute("create table if not exists "+tableprefix+"_group_decisions (groupId int, decision varchar(200), primary key(groupId))");
			stmt.execute("delete from "+tableprefix+"_group_decisions");
		}catch(Exception e){
			e.printStackTrace();
		}		
	}

	/**
	 * empty matrix with the knownstates as the row and column
	 * @param knownstates
	 */
	StateMatrix(Connection conn, String tableprefix, Set<State> knownstates){
		Iterator<State> it = knownstates.iterator();
		while(it.hasNext()){
			State s = (State)it.next();
			states.add(s);
		}
		this.conn = conn;
		Statement stmt = null;
		this.tableprefix = tableprefix;
		try{
			stmt = conn.createStatement();
			stmt.execute("create table if not exists "+tableprefix+"_terms (term varchar(100), cooccurTerm varchar(100), frequency int(4), keep varchar(20), sourceFiles varchar(2000),  primary key(term, cooccurTerm))");
			stmt.execute("delete from "+tableprefix+"_terms");
			stmt.execute("create table if not exists "+tableprefix+"_grouped_terms (groupId int, term varchar(100), cooccurTerm varchar(100), frequency int(4), keep varchar(20), sourceFiles varchar(2000), primary key(term, cooccurTerm))");
			//stmt.execute("delete from terms");
			stmt.execute("create table if not exists "+tableprefix+"_group_decisions (groupId int, category varchar(200), primary key(groupId))");
			//stmt.execute("delete from terms");
		}catch(Exception e){
			e.printStackTrace();
		}
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
			Cell s1s2 = new Cell(i1, i2, new CooccurrenceScore(score, source));
			Cell s2s1 = new Cell(i2, i1, new CooccurrenceScore(score, source));
			matrix.add(s1s2);
			matrix.add(s2s1);
		}
		
		if(f1+f2 == 1){//one new state
			State newstate = f1 == 0? s1: s2;
			State existstate = newstate == s1? s2: s1;
			states.add(newstate);
			int i1 = states.indexOf(existstate);
			int i2 = states.indexOf(newstate);
			Cell en = new Cell(i1, i2, new CooccurrenceScore(score, source));
			Cell ne = new Cell(i2, i1, new CooccurrenceScore(score, source));
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
				CooccurrenceScore cs = e.getScore();
				cs.updateBy(score, source);
			}else{
				matrix.add(new Cell(i1, i2, new CooccurrenceScore(score, source)));
			}
			
			c = new Cell(i2, i1, null);
			cellindex = matrix.indexOf(c);
			if(cellindex >=0){
				Cell e = matrix.get(cellindex);
				CooccurrenceScore cs = e.getScore();
				cs.updateBy(score, source);
			}else{
				matrix.add(new Cell(i2, i1, new CooccurrenceScore(score, source)));
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
	
	public CooccurrenceScore cooccurScore(String str1, String str2){
		State state1 = this.getStateByName(str1);
		State state2 = this.getStateByName(str2);
		int i = states.indexOf(state1);
		int j = states.indexOf(state2);
		Cell c = new Cell(i, j, null);
		if(matrix.contains(c)){
			c = matrix.get(matrix.indexOf(c));
			return c.getScore();//"[]" is an empty score
		}else{
			return null;
		}		
	}
	
	public void save2MySQL(Connection conn, String tableprefix, String username, String password){
		try{
			Statement stmt = conn.createStatement();
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
						CooccurrenceScore score = data.getScore();//"[]" is an empty score
						//String values = "'"+s1.getName()+"','";
						
						String src = score.getSourcesAsString();
						src = src.length() >=2000? src.substring(0, 1999) : src;
						//values +=s2.getName()+"',"+score.getSources().size()+",'"+src+"'";
						String othervalues = score.getSources().size()+",'"+src+"'";
						String[] pair = {s1.getName(), s2.getName()};
						Arrays.sort(pair);
						insertIntoTermsTable(pair, othervalues);
						}
				}				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}	

	private void insertIntoTermsTable(String[] pair, String othervalues) {
		// TODO Auto-generated method stub
		try{
			Statement stmt = conn.createStatement();
			//check to see if the pair exist in terms table
			ResultSet rs = stmt.executeQuery("select * from "+tableprefix+"_terms where term='"+pair[0]+"' and cooccurTerm='"+pair[1]+"'");
			if(!rs.next()){
				stmt.execute("insert into "+tableprefix+"_terms (term, cooccurTerm, frequency, sourceFiles) values('"+pair[0]+"','"+pair[1]+"',"+othervalues+")");
			}
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * group cooccured nodes into groups, using JUNG libary.
	 * 
	 */
	public Object Grouping(){
		//construct the graph
		Graph<State, MyLink> g = new UndirectedSparseMultigraph<State, MyLink>();		
		//state as node
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
		saveClusters(clusters);
		return clusters;
	}
	
	private void saveClusters(Collection clusters){
		int gcount = 1;
		Iterator<Set> sets = clusters.iterator();
		while(sets.hasNext()){
			Set states = (Set)sets.next();
			Iterator<State> sit = states.iterator();
			StringBuffer statestring = new StringBuffer();
			while(sit.hasNext()){
				State s = sit.next();
				statestring.append("'"+s.getName()+"', ");				
			}
			String stategroup = statestring.toString().replaceFirst(", $", "");
			try{
				Statement stmt = conn.createStatement();
				String q = "insert into "+this.tableprefix+"_grouped_terms(term, cooccurTerm, frequency, sourceFiles) (select distinct term, cooccurTerm, frequency, sourceFiles from "+
				this.tableprefix+"_terms where term in ("+stategroup+
				"))";
				stmt.execute(q);
				stmt.execute("update "+this.tableprefix+"_grouped_terms set groupId="+gcount+" where isnull(groupId)");
			}catch(Exception e){
				e.printStackTrace();
			}
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

	public void output2GraphML() {
		GraphMLOutputter gmo = new GraphMLOutputter();
		//from saved grouped_terms
		ArrayList<ArrayList> groups = new ArrayList<ArrayList>();
		int gnumber = 0;		
		try{
			Statement stmt = conn.createStatement();
			String q = "select groupId from "+this.tableprefix+"_grouped_terms order by groupId desc";
			ResultSet rs = stmt.executeQuery(q);
			if(rs.next()){
				gnumber = rs.getInt("groupId");
			}				
			for(int i = 1; i<=gnumber; i++){
				q = "select term, cooccurTerm, frequency from "+this.tableprefix+"_grouped_terms where groupId='"+i+"'";
				rs = stmt.executeQuery(q);
				ArrayList<ArrayList> group = new ArrayList<ArrayList>();
				while(rs.next()){
					ArrayList<String> row = new ArrayList<String>();
					row.add(rs.getString("term"));
					row.add(rs.getString("cooccurTerm"));
					row.add(rs.getString("frequency"));				
					group.add(row);
				}
				rs.close();
				groups.add(group);
			}
		}catch(Exception e){
			e.printStackTrace();
		}			
		
		gmo.output(groups, 1);
	}
					 
}
