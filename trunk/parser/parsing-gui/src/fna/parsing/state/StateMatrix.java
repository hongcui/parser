package fna.parsing.state;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.*;



public class StateMatrix {
	
	private ArrayList<State> states = null;
	private ArrayList<Cell> matrix = null;
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
	 * @param score -1: first, +1: others
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
