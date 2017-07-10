/*******************************************************************************
 * Copyright (c) 2017 Roberto Medina
 * Written by Roberto Medina (rmedina@telecom-paristech.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package fr.tpt.s3.ls_mxc.avail;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import fr.tpt.s3.ls_mxc.alloc.LS;
import fr.tpt.s3.ls_mxc.model.DAG;
import fr.tpt.s3.ls_mxc.model.Node;

public class Automata {

	private int nb_states;
	private List<State> lo_sched;
	private List<State> hi_sched;
	private List<Transition> l_transitions;
	private List<Transition> f_transitions;
	private List<Transition> h_transitions;
	private List<List<AutoBoolean>> l_outs_b;

	private LS ls;
	private DAG d;

	/**
	 *  Constructor of the Automata, needs the LO, HI tables,
	 *  the DAG with the data dependencies, deadline and number of cores
	 */
	public Automata (LS ls, DAG d) {
		this.setD(d);
		this.setLs(ls);
		this.lo_sched = new LinkedList<State>();
		this.hi_sched = new LinkedList<State>();
		this.l_transitions = new LinkedList<Transition>();
		this.setF_transitions(new LinkedList<Transition>());
		this.h_transitions = new LinkedList<Transition>();
		this.l_outs_b = new LinkedList<List<AutoBoolean>>();
	}
	
	/**
	 * Automata functions (creation of states + linking)
	 */
	
	// Calculate completion time of tasks and create a new state
	public void calcCompTimeLO (String task) {
		int c_t = 0;
		for (int i = 0; i < ls.getDeadline(); i++){
			for (int j = 0; j < ls.getNb_cores(); j++) {
				if (ls.getS_LO()[i][j].contentEquals(task))
					c_t = i;
			}
		}

		Node n = d.getNodebyName(task);
		State s;		
		if (n.getC_HI() !=  0) {
			s = new State(nb_states++, task, 1);
			if (n.isfMechanism()) // Test if it's a faul tolerant mechanism
				s.setfMechanism(true);
		} else {
			s = new State(nb_states++, task, 0);
		}
		s.setC_t(c_t);
		addWithTime(lo_sched, n, s, c_t);
	}
	
	// Calculate completion time of tasks and create a new state HI mode
	public void calcCompTimeHI (String task) {
		int c_t = 0;
		for (int i = 0; i < ls.getDeadline(); i++){
			for (int j = 0; j < ls.getNb_cores(); j++) {
				if (ls.getS_HI()[i][j].contentEquals(task))
					c_t = i;
			}
		}

		Node n = d.getNodebyName(task);
		State s;
		s = new State(nb_states++, task, 0);
		s.setC_t(c_t);

		addWithTime(hi_sched, n, s, c_t);
	}
	
	/**
	 * Procedure that adds the state to a list in the right order.
	 * @param l
	 * @param n
	 * @param s
	 * @param c_t
	 */
	public void addWithTime(List<State> l, Node n, State s, int c_t) {
		int idx = 0;
		Iterator<State> is = l.iterator();
		State s2 = null;
		
		// Iterate until the first task that has the same or a higher completion time
		while (is.hasNext()) {
			s2 = is.next();
			if (s2.getC_t() >= c_t)
				break;
			else
				idx++;
		}
		
		// Breaking ties, LO exit nodes first
		// then HI tasks, then LO tasks
		if (s2 != null && s2.getC_t() == c_t) {
			
			if (n.getC_HI() != 0) {	
				int cur_ct = c_t;
				while (is.hasNext() && cur_ct == c_t) {
					s2 = is.next();
					cur_ct = s2.getC_t();
					if (s2.getMode() == 1)
						idx++;
				}
			} else if (n.getC_HI() == 0) {
				System.out.println("Equality "+n.getName());
				int cur_ct = c_t;
				while (is.hasNext() && cur_ct == c_t) {
					s2 = is.next();
					cur_ct = s2.getC_t();
					idx++;
				}
			}
		}
		System.out.println("Task "+n.getName()+" index in list "+idx);
		l.add(idx,s);
	}
	
	/**
	 * Finds the corresponding state of a LO task in LO automata zone
	 * @param task
	 * @return
	 */
	public State findStateLO(String task) {
		State ret = null;
		boolean found = false;
		
		Iterator<State> it = lo_sched.iterator();
		while (it.hasNext() && !found) {
			State s = it.next();
			if (s.getTask().contentEquals(task)) {
				ret = s;
				found = true;
			}
		}
		
		return ret;
	}
	
	/**
	 * Finds the corresponding state of a HI task in HI automata zone
	 * @param task
	 * @return
	 */
	public State findStateHI(String task) {
		State ret = null;
		boolean found = false;
		
		Iterator<State> it = hi_sched.iterator();
		while (it.hasNext() && !found) {
			State s = it.next();
			if (s.getTask().contentEquals(task)) {
				ret = s;
				found = true;
			}
		}
		return ret;
	}
	
	/**
	 * Procedure that calculates all the sets of booleans
	 * for each output in the DAG
	 */
	public void calcOutputSets() {
		Iterator<Node> in = d.getLO_outs().iterator();
		while (in.hasNext()) {
			Node n = in.next();
			Set<Node> nPred = n.getLOPred();
			
			// Create the boolean set for the LO output
			LinkedList<AutoBoolean> bSet = new LinkedList<AutoBoolean>();
			AutoBoolean a = new AutoBoolean(n.getName());
			bSet.add(a);
			Iterator<Node> in2 = nPred.iterator();
			while (in2.hasNext()) {
				Node n2 = in2.next();
				AutoBoolean ab = new AutoBoolean(n2.getName());
				bSet.add(ab);
			}
			l_outs_b.add(bSet);
		}
	}
	
	/**
	 * Procedure links the states by creating Transitions objects
	 * after the scheduling lists were created.
	 */
	public void linkStates() {
		
		Iterator<State> it = hi_sched.iterator();
		Iterator<State> it2 = hi_sched.iterator();

		// Construct the HI zone of the automata
		State s2 = it2.next(); 
		while (it2.hasNext()) {
			State s = it.next();
			if (it2.hasNext()) {
				s2 = it2.next();
				Transition t = new Transition(s, s2, null);
				getH_transitions().add(t);
			}
		}
				
		// Construct the LO zone of the automata
		State sk = new State(nb_states++, "FinalLO", 0);
		lo_sched.add(sk);
		it = lo_sched.iterator();
		it2 = lo_sched.iterator();
		s2 = it2.next();
		while (it2.hasNext()) {
			State s = it.next();
			if (it2.hasNext()) {
				s2 = it2.next();
				Transition t;
				if (s.getMode() == 1) { // If it's a HI task
					// Find the HI task that corresponds to s
					State S = findStateHI(s.getTask());
					t = new Transition(s, s2, S);
					if(!s.isfMechanism()) // If it's a fault tolerant mechanism
						t.setP(d.getNodebyName(s.getTask()).getfProb());
				} else { // It is a LO task
					t = new Transition(s, s2, s2);
					t.setP(d.getNodebyName(s.getTask()).getfProb());
				}
				getL_transitions().add(t);
			}
		}
		
		// Add final transition in HI mode (recovery mechanism)
		State s0 = lo_sched.get(0);
		State Sf = hi_sched.get(hi_sched.size() - 1);
		Transition t = new Transition(Sf, s0, null);
		getH_transitions().add(t);
		
		// Add final transitions in LO mode
		// We need to add 2^n transitions depending on the number of outputs
		calcOutputSets();
		boolean finished = false;
		int max_depth = l_outs_b.size();
		int curr = max_depth - 1;
		int idx = 0;
		Iterator<List<AutoBoolean>> ib = l_outs_b.listIterator();
		
		
		while (ib.hasNext()) {
			List<AutoBoolean> sab0 = ib.next();
			while (!finished) {
				int idx2 = idx + 1;
				// Grab next element(s) when curr depth != 0
				while (curr != 0) {
					Transition t2 = new Transition(sk, s0, s0);
					t2.getbSet().addAll(sab0);					
					for (int i = idx2; i < idx2 + curr; i++) {
						List<AutoBoolean> sab = l_outs_b.get(i);
						t2.getbSet().addAll(sab);
					}
					this.f_transitions.add(t2);
					
					if ((idx2 + curr) == l_outs_b.size()) // Added all the elements 
						curr--; // Reduce size of the set
					else // Else move to the next element
						idx2++;
				}

				if (curr == 0) 
					finished = true;
			}
			idx++;
			max_depth--;
			curr = max_depth - 1;
			finished = false;
		}
		
		// Add individual exits
		ib = l_outs_b.listIterator();
		while (ib.hasNext()) {
			List<AutoBoolean> sab0 = ib.next();
			Transition t2 = new Transition(sk, s0, s0);
			t2.getbSet().addAll(sab0);
			this.f_transitions.add(t2);

		}
	}
	

	/**
	 *  This procedures prints the automata
	 */
	public void createAutomata () {
		
		// Calculate completion times for all nodes in LO and HI mode
		
		Iterator<Node> in = d.getNodes().iterator();
		while (in.hasNext()) {
			Node n = in.next();
			this.calcCompTimeLO(n.getName());
		}
		
		in = d.getNodes_HI().iterator();
		while (in.hasNext()) {
			Node n = in.next();
			this.calcCompTimeHI(n.getName());
		}
		
		System.out.println("Completition times calculated!");
		
		this.linkStates();
		
		System.out.println("Linked states!");

		
		System.out.println("module proc");
		System.out.println("\ts : [0..50] init 0");
		
		// Create all necessary booleans
		Iterator<State> is = lo_sched.iterator();
		while (is.hasNext()) {
			State s = is.next();
			if (s.getMode() == 0 && !s.getTask().contains("Final")) // It is a LO task
				System.out.println("\t"+s.getTask()+"bool: bool init false;");
		}
		
		System.out.println("");
		
		// Create the LO scheduling zone
		Iterator<Transition> it = l_transitions.iterator();
		while (it.hasNext()) {
			Transition t = it.next();
			if (t.getSrc().getMode() == 1) {
				if (! t.getSrc().isfMechanism())
					System.out.println("\t["+t.getSrc().getTask()+"_lo] s = " + t.getSrc().getId()
							+ " -> 1 - "+ t.getP() +" : (s' = " + t.getDestOk().getId() + ") +"
							+ t.getP() + ": (s' =" + t.getDestFail().getId() +");");
				else {
					System.out.println("\t["+t.getSrc().getTask()+"_ok] s = " + t.getSrc().getId()
							+ " -> : (s' = " + t.getDestOk().getId() + ");");
					System.out.println("\t["+t.getSrc().getTask()+"_fail] s = " + t.getSrc().getId()
							+ " -> : (s' = " + t.getDestFail().getId() + ");");
				}
			} else { // If it's a LO task we need to update the boolean
				System.out.println("\t["+t.getSrc().getTask()+"_lo] s = " + t.getSrc().getId()
						+ " -> 1 - "+ t.getP() +" : (s' = " + t.getDestOk().getId() +") & ("+t.getSrc().getTask()+"bool' = true) + "
						+ t.getP() + ": (s' =" + t.getDestFail().getId() + ");" );
			}
		}
		
		// Create the 2^n transitions for the end of LO
		Iterator<Transition> itf = f_transitions.iterator();
		int curr = 0;
		while (itf.hasNext()) {
			Transition t = itf.next();
			System.out.print("\t["+t.getSrc().getTask()+curr+"] s = " + t.getSrc().getId());
			Iterator<AutoBoolean> ib = t.getbSet().iterator();
			while(ib.hasNext()) {
				AutoBoolean ab = ib.next();
				System.out.print(" & " + ab.getTask()+"bool = true");
			}
			System.out.println(" -> (s' = "+t.getDestOk().getId()+");");
			curr++;
		}
		

		// Create the HI scheduling zone
		// Need to iterate through transitions
		System.out.println("");
		it = h_transitions.iterator();
		while (it.hasNext()) {
			Transition t = it.next();
			System.out.println("\t["+t.getSrc().getTask()+"_hi] s = " + t.getSrc().getId() + " -> (s' =" + t.getDestOk().getId() +");");
		}
		
		System.out.println("end module;");
		
		// Create the rewards
		System.out.println("");
		in = d.getLO_outs().iterator();
		while (in.hasNext()) {
			Node n = in.next();
			System.out.println("rewards \""+n.getName()+"_cycles\"");
			it = f_transitions.iterator();
			int c = 0;
			while (it.hasNext()) {
				Transition t = it.next();
				Iterator<AutoBoolean> iab = t.getbSet().iterator();

				while (iab.hasNext()) {
					if (iab.next().getTask().contentEquals(n.getName()))
						System.out.println("\t["+t.getSrc().getTask()+c+"] true : 1;");
				}
				c++;
			}
			c = 0;
			System.out.println("endrewards");
			System.out.println("");
		}
	}
	
	/**
	 * Print functions
	 */
	public void printLOList() {
		Iterator<State> it = lo_sched.iterator();
		while (it.hasNext()){
			System.out.print(it.next().getTask()+ " ");
		}		
		System.out.println(" ");
	}
	
	public void printHIList() {
		Iterator<State> it = hi_sched.iterator();
		while (it.hasNext()){
			System.out.print(it.next().getTask()+ " ");
		}
		System.out.println(" ");
	}
	
	/**
	 * Getters and setters
	 */

	public List<State> getLo_sched() {
		return lo_sched;
	}

	public void setLo_sched(List<State> lo_sched) {
		this.lo_sched = lo_sched;
	}

	public List<State> getHi_sched() {
		return hi_sched;
	}

	public void setHi_sched(List<State> hi_sched) {
		this.hi_sched = hi_sched;
	}

	public List<Transition> getL_transitions() {
		return l_transitions;
	}

	public void setL_transitions(List<Transition> l_transitions) {
		this.l_transitions = l_transitions;
	}

	public List<Transition> getH_transitions() {
		return h_transitions;
	}

	public void setH_transitions(List<Transition> h_transitions) {
		this.h_transitions = h_transitions;
	}

	public DAG getD() {
		return d;
	}

	public void setD(DAG d) {
		this.d = d;
	}

	public LS getLs() {
		return ls;
	}

	public void setLs(LS ls) {
		this.ls = ls;
	}

	public List<List<AutoBoolean>> getL_outs_b() {
		return l_outs_b;
	}

	public void setL_outs_b(List<List<AutoBoolean>> l_outs_b) {
		this.l_outs_b = l_outs_b;
	}

	public List<Transition> getF_transitions() {
		return f_transitions;
	}

	public void setF_transitions(List<Transition> f_transitions) {
		this.f_transitions = f_transitions;
	}
}
