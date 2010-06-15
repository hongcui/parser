package fna.parsing.state;

import java.util.*;

public class Cell implements Comparator<Object>{
	private int r = -1;
	private int c = -1;
	private CoocurrenceScore score = null;
	
	Cell(){
		
	}
	
	Cell(int r, int c){
		this.r = r;
		this.c = c;
	}

	Cell(int r, int c, CoocurrenceScore score){
		this.r = r;
		this.c = c;
		this.score = score;
	}
	
	public int getRindex(){
		return r;
	}
	
	public int getCindex(){
		return c;
	}
	public CoocurrenceScore getScore(){
		return score;
	}
	
	public int compare(Object o1, Object o2){
		Cell c1 = (Cell)o1;
		Cell c2 = (Cell)o2;
		int rdiff = c1.getRindex() - c2.getRindex();
		int cdiff = c1.getCindex() - c2.getCindex();
		if(rdiff != 0){
			return rdiff;
		}else{
			if(cdiff != 0){
				return cdiff;
			}else{
				return 0;
			}
		}
	}
	
	public boolean equals(Object c){
		if(((Cell)c).getCindex() == this.c && ((Cell)c).getRindex()==this.r){
			return true;
		}
		return false;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		String s = score ==null? "" : score.toString();
		sb.append("("+r+","+c+")="+s);
		return sb.toString();
	}
}
