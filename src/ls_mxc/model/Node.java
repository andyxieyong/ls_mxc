package ls_mxc.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Node {
	
	private int id;
	private String name;
	
	private int C_LO;
	private int C_HI;
	
	private boolean source;
	private boolean sink;
	
	private int weight_LO;
	private int weight_HI;
	
	private Set<Edge> rcv_edges;
	private Set<Edge> snd_edges;
	
	// Used for DAG generation
	private int rank;
	private int cpFromNode_LO;
	private int cpFromNode_HI;
	
	/**
	 * Constructors
	 */
	public Node(int id, String name, int c_lo, int c_hi){
		this.setId(id);
		this.setC_LO(c_lo);
		this.setC_HI(c_hi);
		this.setName(name);
		this.setSink(false);
		this.setSource(false);
		
		rcv_edges = new HashSet<Edge>();
		snd_edges = new HashSet<Edge>();
	}
	
	/**
	 * 	Utility methods
	 */
	public void checkifSource() {
		if (rcv_edges.size() == 0)
			this.setSource(true);
	}
	
	public void checkifSink() {
		if (snd_edges.size() == 0)
			this.setSink(true);
	}
	
	/**
	 * 
	 * @param n
	 * @param mode
	 * @return
	 */
	public int CPfromNode (int mode) {
		
		if (this.getRcv_edges().size() == 0) {
			if (mode == 0) {
				this.setCpFromNode_LO(C_LO);
				return this.getC_LO();
			} else {
				this.setCpFromNode_HI(C_HI);
				return this.getC_HI();
			}
		} else {
			int max = 0;
			int tmp = 0;
			Iterator<Edge> it_e = this.getRcv_edges().iterator();
			while (it_e.hasNext()){
				Edge e = it_e.next();
				if (mode == 0) {
					tmp = e.getSrc().CPfromNode(0);
					if (max < tmp)
						max = tmp + this.getC_LO();
				} else {
					tmp = e.getSrc().CPfromNode(1);
					if (max < tmp)
						max = tmp + this.getC_HI();
				}
			}
			if (mode == 0)
				this.setCpFromNode_LO(max);
			else
				this.setCpFromNode_HI(max);
			
			return max;
		}
			
	}

	
	/**
	 *  Getters & Setters
	 */
	public int getC_LO() {
		return C_LO;
	}
	public void setC_LO(int c_LO) {
		C_LO = c_LO;
	}
	public int getC_HI() {
		return C_HI;
	}
	public void setC_HI(int c_HI) {
		C_HI = c_HI;
	}
	public boolean isSource() {
		return source;
	}
	public void setSource(boolean source) {
		this.source = source;
	}
	public boolean isSink() {
		return sink;
	}
	public void setSink(boolean sink) {
		this.sink = sink;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public Set<Edge> getRcv_edges() {
		return rcv_edges;
	}
	public void setRcv_edges(Set<Edge> rcv_edges) {
		this.rcv_edges = rcv_edges;
	}
	public Set<Edge> getSnd_edges() {
		return snd_edges;
	}
	public void setSnd_edges(Set<Edge> snd_edges) {
		this.snd_edges = snd_edges;
	}
	public int getWeight_LO(){
		return this.weight_LO;
	}
	public void setWeight_LO(int w_lo){
		this.weight_LO = w_lo;
	}
	public int getWeight_HI(){
		return this.weight_HI;
	}
	public void setWeight_HI(int w_hi){
		this.weight_HI = w_hi;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public int getCpFromNode_LO() {
		return cpFromNode_LO;
	}

	public void setCpFromNode_LO(int cpFromNode_LO) {
		this.cpFromNode_LO = cpFromNode_LO;
	}

	public int getCpFromNode_HI() {
		return cpFromNode_HI;
	}

	public void setCpFromNode_HI(int cpFromNode_HI) {
		this.cpFromNode_HI = cpFromNode_HI;
	}


}
