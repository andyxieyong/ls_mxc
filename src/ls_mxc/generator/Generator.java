package ls_mxc.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import ls_mxc.model.DAG;
import ls_mxc.model.Edge;
import ls_mxc.model.Node;

/**
 * Generates random graphs to schedule 
 * @author Roberto Medina
 *
 */
public class Generator {

	/**
	 * Variables
	 */
	private int nbNodes;
	private int nbCores;
	private int MIN_RANKS;
	private int MAX_RANKS;
	private int MIN_PER_RANK;
	private int MAX_PER_RANK;
	
	private int deadline;
	
	private float edgeProb;
	private float hiPerc;
	private DAG d;
	
	private int[][] adjMatrix;
	
	public Generator(int height, int width, int eprob, int hperc) {
		this.setEdgeProb(eprob);
		this.setHiPerc(hperc);
				
		MIN_RANKS = 2;
		MAX_RANKS = height;
		MIN_PER_RANK = 1;
		MAX_PER_RANK = width;
	}
	
	/**
	 * Graph generation should avoid 2 things:
	 *  - Making cycles with the edges
	 *  - Creating an edge from a LO task to a HI task
	 */
	public void generateGraph(){
		
		
		Set<Node> nodes = new HashSet<Node>();
		Set<Node> new_nodes = new HashSet<Node>();
		Random r = new Random();
		int nodes_created = 0;
		int id = 0;
		d = new DAG();
		
		// How "tall" the DAG should be
		
		int ranks = r.nextInt(MAX_RANKS - MIN_RANKS) + MIN_RANKS;
				
		// For each level of the DAG
		for (int i = 0; i < ranks; i++) {
			
			// Generate a new node with a higher rank than the one previously made
			// And its parameters
			int nb_nodes_rank = r.nextInt(MAX_PER_RANK - MIN_PER_RANK) + MIN_PER_RANK;
			
			for(int j = 0; j < nb_nodes_rank; j++) {
			
				Node n = new Node(id, Character.toString((char) ((char)'A'+ id )), 0, 0);
				n.setRank(i);
				n.setC_LO(r.nextInt(4) + 1);
				
				if ((r.nextInt() % 100) < hiPerc || (id == 0)) // At least one source is HI
					n.setC_HI((int) (n.getC_LO() * 1.5));
				else
					n.setC_HI(0);
				
				new_nodes.add(n);
				id++;
				nodes_created++;
			}
		
			
			// Iterate through lower ranks and add an edge only from low rank to higher one
			Iterator<Node> it_n = nodes.iterator();
			while (it_n.hasNext()){
				Iterator<Node> it_n2 = new_nodes.iterator();
				Node src = it_n.next();
				while (it_n2.hasNext()){
					Node dest = it_n2.next();
					
					// Probably of adding an edge between the 2 nodes
					if (r.nextInt(100) <= edgeProb){
						// Check that it's not a LO->HI communication
						if ((src.getC_HI() > 0 && dest.getC_HI() >= 0) ||
								(src.getC_HI() == 0 && dest.getC_HI() == 0)) {
							Edge e = new Edge(src, dest, false);
							src.getSnd_edges().add(e);
							dest.getRcv_edges().add(e);
						}
					}
				}
			}
			
			// Add the "new" nodes to the set
			Iterator<Node> it_n2 = new_nodes.iterator();
			while (it_n2.hasNext()){
				Node n = it_n2.next();
				nodes.add(n);
				it_n2.remove();
			}
			
		}
		setNbNodes(nodes_created);
		d.setNodes(nodes);
		this.setDeadline(d.calcCriticalPath());
		calcMinCores();
		graphSanityCheck();
		createAdjMatrix();
	}
	
	
	/**
	 * Sanity check for the graph:
	 * 	- Each node has to have at least one edge
	 */
	public void graphSanityCheck() {
		boolean added = false;
		Iterator<Node> it_n = d.getNodes().iterator();
		
		while (it_n.hasNext()) {
			Node n = it_n.next();
			
			// It is an independent node with no edges
			if (n.getRcv_edges().size() == 0 && n.getSnd_edges().size() == 0) {
				Iterator<Node> it_n2 = d.getNodes().iterator();
				while (it_n2.hasNext() && added == false) {
					Node n2 = it_n2.next();
					if ((n.getRank() < n2.getRank()) &&
							((n.getC_HI() > 0 && n2.getC_HI() > 0) ||
							 (n.getC_HI() == 0 && n2.getC_HI() == 0) ||
							 (n.getC_HI() > 0 && n2.getC_HI() == 0))){
						Edge e = new Edge(n, n2, false);
						n.getSnd_edges().add(e);
						n2.getRcv_edges().add(e);
						added = true;
					} else if (n.getRank() > n2.getRank() &&
							((n.getC_HI() > 0 && n2.getC_HI() > 0) ||
							 (n.getC_HI() == 0 && n2.getC_HI() == 0) ||
							 (n.getC_HI() == 0 && n2.getC_HI() > 0))) {
						Edge e = new Edge(n2, n, false);
						n.getRcv_edges().add(e);
						n2.getSnd_edges().add(e);
						added = true;
					}
				}
			}
		}
	}
	
	/**
	 * Calculate the minimum number of cores for the Graph.
	 */
	public void calcMinCores() {
		int sumClo = 0;
		int sumChi = 0;
		int max;
		
		Iterator<Node> it_n = d.getNodes().iterator();
		
		while (it_n.hasNext()) {
			Node n = it_n.next();
			sumChi += n.getC_HI();
			sumClo += n.getC_LO();
		}
		
		if (sumChi >= sumClo)
			max = sumChi;
		else
			max = sumClo;
		
		this.setNbCores((int)Math.ceil(max/this.getDeadline()));
	}
	
	/**
	 * Creates the matrix to be written in the files
	 */
	public void createAdjMatrix(){
		adjMatrix = new int[nbNodes][];
		for (int i = 0; i < nbNodes; i++)
			adjMatrix[i] = new int[nbNodes];
		
		Iterator<Node> it_n = d.getNodes().iterator();
		while (it_n.hasNext()){
			Node n = it_n.next();
			
			Iterator<Edge> it_e = n.getRcv_edges().iterator();
			while (it_e.hasNext()){
				Edge e = it_e.next();
				adjMatrix[e.getDest().getId()][e.getSrc().getId()] = 1;
			}
		}
	}
	
	/**
	 * Writes the generated Graph into a file that can be used by the allocator
	 * @param filename
	 * @throws IOException
	 */
	public void toFile(String filename) throws IOException{
		
		BufferedWriter out = null;
		try {
			File f = new File(filename);
			f.createNewFile();
			FileWriter fstream = new FileWriter(f);
			out = new BufferedWriter(fstream);
			
			// Write number of nodes
			out.write("#NbNodes\n");
			out.write(Integer.toString(this.getNbNodes()) + "\n\n");
			
			// Write number of cores
			out.write("#NbCores\n");
			out.write(Integer.toString(this.getNbCores()) + "\n\n");
			
			// Write number of cores
			out.write("#Deadline\n");
			out.write(Integer.toString(this.getDeadline()) + "\n\n");
			
			//Write C LOs
			out.write("#C_LO\n");
			for (int i = 0; i < nbNodes; i++) {
				Node n = d.getNodebyID(i);
				out.write(Integer.toString(n.getC_LO()) + "\n");
			}
			out.write("\n");
			
			//Write C HIs
			out.write("#C_HI\n");
			for (int i = 0; i < nbNodes; i++) {
				Node n = d.getNodebyID(i);
				out.write(Integer.toString(n.getC_HI()) + "\n");
			}
			out.write("\n");
			
			//Write precedence matrix
			out.write("#Pred\n");
			for (int i = 0; i < nbNodes; i++) {
				for (int j = 0; j < nbNodes; j++){
					out.write(Integer.toString(adjMatrix[i][j]));
					if (j < nbNodes - 1)
						out.write(",");
				}
				out.write("\n");
			}
			out.write("\n");
			
		}catch (IOException e){
			System.out.print("To File : " + e.getMessage());
		}finally{
			if(out != null)
				out.close();
		}
	}
	
	/**
	 * Write the DAG to a .dzn file for the CSP
	 * @param filename
	 * @throws IOException
	 */
	public void toDZN(String filename) throws IOException {
		BufferedWriter out = null;
		try {
			File f = new File(filename);
			f.createNewFile();
			FileWriter fstream = new FileWriter(f);
			out = new BufferedWriter(fstream);
			
			// Write number of nodes
			out.write("NbNodes = ");
			out.write(Integer.toString(this.getNbNodes()) + ";\n");
			
			// Write number of cores
			out.write("NbCores = ");
			out.write(Integer.toString(this.getNbCores()) + ";\n");
			
			// Write number of cores
			out.write("Deadline = ");
			out.write(Integer.toString(this.getDeadline()) + ";\n");
			
			// Max slots
			out.write("MaxSlot = ");
			out.write(Integer.toString(this.getDeadline()) + ";\n\n");
			
			//Write C LOs
			out.write("C_LO = [");
			for (int i = 0; i < nbNodes; i++) {
				Node n = d.getNodebyID(i);
				out.write(Integer.toString(n.getC_LO()));
				if (i != nbNodes - 1)
					out.write(", ");
			}
			out.write("];\n\n");
			
			//Write C HIs
			out.write("C_HI = [");
			for (int i = 0; i < nbNodes; i++) {
				Node n = d.getNodebyID(i);
				out.write(Integer.toString(n.getC_HI()));
				if (i != nbNodes - 1)
					out.write(", ");
			}
			out.write("];\n\n");
			
			//Write precedence matrix
			out.write("Pred = [");
			for (int i = 0; i < nbNodes; i++) {
				out.write("| ");
				for (int j = 0; j < nbNodes; j++){
					out.write(Integer.toString(adjMatrix[i][j]));
					if (j < nbNodes - 1)
						out.write(", ");
				}
				if (i != nbNodes - 1)
					out.write("\n");
			}
			out.write("|];\n");
			
		}catch (IOException e ){
			System.out.print("Exception " + e.getMessage());
		}finally{
			if(out != null)
				out.close();
		}
	}
	
	/**
	 * Getters and setters
	 */
	public int getNbNodes() {
		return nbNodes;
	}
	public void setNbNodes(int nbNodes) {
		this.nbNodes = nbNodes;
	}
	public int getNbCores() {
		return nbCores;
	}
	public void setNbCores(int nbCores) {
		this.nbCores = nbCores;
	}
	public float getEdgeProb() {
		return edgeProb;
	}
	public void setEdgeProb(float edgeProb) {
		this.edgeProb = edgeProb;
	}
	public float getHiPerc() {
		return hiPerc;
	}
	public void setHiPerc(float hiPerc) {
		this.hiPerc = hiPerc;
	}

	public int[][] getAdjMatrix() {
		return adjMatrix;
	}

	public void setAdjMatrix(int[][] adjMatrix) {
		this.adjMatrix = adjMatrix;
	}

	public int getDeadline() {
		return deadline;
	}

	public void setDeadline(int deadline) {
		this.deadline = deadline;
	}
	public DAG getD() {
		return d;
	}

	public void setD(DAG d) {
		this.d = d;
	}
	
	
}
