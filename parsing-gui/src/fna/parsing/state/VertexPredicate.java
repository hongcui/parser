package fna.parsing.state;

import java.util.Collection;

import org.apache.commons.collections15.Predicate;
import edu.uci.ics.jung.graph.*;

public class VertexPredicate<State>  implements Predicate<State>{
			Graph g;
		public VertexPredicate(Graph g){
			this.g = g;
		}
		public boolean evaluate(State s) {
			return g.degree(s) > 1;
		} 
		
}
