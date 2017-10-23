/*******************************************************************************
 * Copyright (c) 2017 Roberto Medina
 * Written by Roberto Medina (rmedina@telecom-paristech.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package fr.tpt.s3.ls_mxc.generator;

import java.io.IOException;
import java.util.Random;

import fr.tpt.s3.ls_mxc.parser.MCParser;

public class GeneratorThread implements Runnable{

	private UtilizationGenerator ug;
	private MCParser mcp;
	private boolean debug;

	public GeneratorThread (double uLO, double uHI, int cp, int edProb,
			double uHIinLO, int para, int cores, int nbDags, String outFile, boolean debug) {
		ug = new UtilizationGenerator(uLO, uHI, cp, edProb, uHIinLO, para, cores, nbDags, debug);
		mcp = new MCParser(outFile, ug);
		setDebug(debug);
	}
	
	@Override
	public void run() {
		Random r = new Random();
		
		for (int i = 0; i < ug.getNbDags(); i++) {
			if (isDebug()) System.out.println("[DEBUG] Generating DAG #"+i);
		
			if (r.nextInt(10) % 2 == 0)
				ug.GenenrateGraph();
			else
				ug.GenenrateGraphCp();
		}

		// Write the file
		try {
			mcp.writeGennedDAG();
			System.out.println(Thread.currentThread().getName()+"> Written generated DAG(s)!");
		} catch (IOException e) {
			System.err.println("[ERROR] Failed to write the XML file in the generator " + e.getMessage());
			System.exit(1);
			return;
		}
	}

	/*
	 * Getters & setters
	 */
	public UtilizationGenerator getUg() {
		return ug;
	}

	public void setUg(UtilizationGenerator ug) {
		this.ug = ug;
	}
	
	public MCParser getMcp() {
		return mcp;
	}

	public void setMcp(MCParser mcp) {
		this.mcp = mcp;
	}
	
	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}
}
