package pstkm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import ilog.concert.*;
import ilog.cplex.*;

public class Model {
	
	// Temp, pozniej beda wczytywane dane wejsciowe
	
	
	//	Zbiory
	//Integer [] D = new Integer[d_len];	//	Zbior zapotrzebowan
	Integer [] D;
	Integer [] V;	//	Zbior wezlow
	Path [] P;		//	Zbior sciezek
	
	
	// Wartosci stale
	Integer [][][] delta_vdp;
	Integer [][] s_vd;
	Integer [][] t_vd;
	Integer [] T_v;				// [v], maksymalna szybkosc z jaka moze wysylac dane wezel, T_v >= 0
	Integer [] R_v;				// [v], maksymalna szybkosc z jaka moze odbierac dane wezel, R_v >= 0
	Double [] pc_v;				// [v], wsp. zuzycia energii wezla v
	
	Integer k;					// k ścieżek będzie generowane dla każdego zapotrzebowania d
	Double r;
	Double [] R = {0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
	Double [] results = new Double [R.length];
	
	Paths paths = new Paths();
	
	
	public Model() {
		readData();
		fill_delta_s_t();	
		for (int i = 0; i < R.length; i++) {
			results[i] = model(R[i]);
		}
		for (int i = 0; i < R.length; i++) {
			System.out.println("Wsp. r = "+R[i]+" --> h_d = "+results[i]);
		}
	}
	
	public Double model(Double _r) {
		Double result = null;
		try {
			IloCplex cplex = new IloCplex();
			
			r = 1 - _r;
			
			// Zmienne
			/// Definicja zmiennej x_dp, ruch zapotrzebowania d na sciezce p, x_dp >= 0
			IloIntVar [][] x_dp = new IloIntVar[D.length][P.length];
			for (Path p : P) {
				x_dp[p.getD()-1][p.getNr()-1] = cplex.intVar(0, Integer.MAX_VALUE, "x_"+p.getD()+"-"+p.getNr());
			}
			
			/// Definicja zmiennej h_d, volumen zapotrzebowania d, h_d >= 0
			IloIntVar h_d = cplex.intVar(0, Integer.MAX_VALUE, "h_d");

			
			// Cel
			//cplex.addMaximize(h_d);
			IloLinearIntExpr obj = cplex.linearIntExpr();
			for (Integer d : D) {				
				for (Path p : P) {
					if (x_dp[d-1][p.getNr()-1] != null) {
						obj.addTerm(1, x_dp[d - 1][p.getNr() - 1]);
					}
				}
			}
			cplex.addMaximize(h_d);
			
			
			// Ograniczenia
			/// Zapotrzebowanie musi być zrealizowane w całości
			for (Integer d : D) {
				IloLinearIntExpr lhs = cplex.linearIntExpr();
				for (Path p : P) {
					if (x_dp[d-1][p.getNr()-1] != null) {
						lhs.addTerm(1, x_dp[d - 1][p.getNr() - 1]);
					}
				}
				cplex.addEq(lhs, h_d);
			}
			
			/// Ograniczenie na maksymalną szybkość wysyłania danych przez węzeł 
			for (Integer v : V) {
				IloLinearIntExpr lhs = cplex.linearIntExpr();
				for (Integer d : D) {
					for (Path p : P) { 
						Integer temp = delta_vdp[v-1][d-1][p.getNr()-1] * t_vd[v-1][d-1];
						if (temp != 0) {
							if (x_dp[d-1][p.getNr()-1] != null) {
								lhs.addTerm(temp, x_dp[d - 1][p.getNr() - 1]);
							}
						}
					}
				}
				cplex.addLe(lhs, T_v[v-1]*r);
			}
			
			/// Ograniczenie na maksymalną szybkość odbierania danych przez węzeł  
			for (Integer v : V) {
				IloLinearIntExpr lhs = cplex.linearIntExpr();
				for (Integer d : D) {
					for (Path p : P) {
						Integer temp = delta_vdp[v-1][d-1][p.getNr()-1] * s_vd[v-1][d-1];
						if (temp != 0) {
							if (x_dp[d-1][p.getNr()-1] != null) {
								lhs.addTerm(temp, x_dp[d - 1][p.getNr() - 1]);
							}
						}
					}
				}
				cplex.addLe(lhs, R_v[v-1]);
			}
			
			/// Ograniczenie na maksymalne zużycie energii przez węzeł 
			for (Integer v : V) {
				IloLinearIntExpr lhs = cplex.linearIntExpr();
				for (Integer d : D) {
					for (Path p : P) {
						Integer temp = delta_vdp[v-1][d-1][p.getNr()-1] * (s_vd[v-1][d-1] + t_vd[v-1][d-1]);
						if (temp != 0) {
							if (x_dp[d-1][p.getNr()-1] != null) {
								lhs.addTerm(temp, x_dp[d - 1][p.getNr() - 1]);
							}
						}
					}
				}
				Double d = ((double)T_v[v-1]*r + (double)R_v[v-1]) * pc_v[v-1];
				Integer rhs = d.intValue();
				cplex.addLe(lhs, rhs);
			}
			
			if (cplex.solve()) {
				System.out.println("obj = "+cplex.getObjValue());
				result = cplex.getObjValue();
			}
			else {
				System.out.println("Model not solved");
				result = -1.0;
			}
			
		} 
		catch (IloException exc) {
			exc.printStackTrace();
		}
		
		return result;
	}
	
	
	private void fill_delta_s_t() {
		delta_vdp = new Integer[V.length][D.length][P.length];
		s_vd = new Integer[V.length][D.length];
		t_vd = new Integer[V.length][D.length];
		
		for (Integer d : D) {
			for (Integer v : V) {
				s_vd[v-1][d-1] = 1;
				t_vd[v-1][d-1] = 1;
				for (Path p : P) {
					for (Integer v_p : p.getAllV()) {
						delta_vdp[v-1][d-1][p.getNr()-1] = 0;
					}
				}
				for (Path p : P) {
					if (p.getD() == d && p.getStartV() == v) {
						s_vd[v-1][d-1] = 0;
					} 
					if (p.getD() == d && p.getEndV() == v) {
						t_vd[v-1][d-1] = 0;
					} 
					for (Integer v_p : p.getAllV()) {
						if (v_p == v && p.getD() == d) {
							delta_vdp[v-1][d-1][p.getNr()-1] = 1;
						}
					}
				}
			}
		}
	}
	
	private void readData() {
		System.out.println("Należy wybrać jeden z predefiniowanych przykładów.");
		System.out.println("Jako nazwę pliku z przykładem należy wpisać: \n"
				+ "\"trywialny1.txt\", \"nietrywialny1.txt\" lub \"nietrywialny2.txt\" ");
		System.out.println("Pliki znajdują się w folderze z projektem. \n");
		
		String filename = "";
		Scanner input = new Scanner(System.in);
		System.out.println("Prosze podac nazwe pliku z przykladem:");
		filename = input.nextLine();
		input.close();
		if (filename.equals("trywialny1.txt") || filename.equals("nietrywialny1.txt") || filename.equals("nietrywialny2.txt")) {
			System.out.println("Wybrany przyklad: "+filename);
			readExampleFromFile(filename);
		}
		else {
			System.out.println("Niewybrano przykladu lub wybrany przykład nie istnieje. \n"
					+ "Obliczony będzie przyklad \"trywialny1.txt\"");
			readExampleFromFile("trywialny1.txt");
		}
		
		// Generacja i dodanie ścieżek do modelu
		paths.modelReady();
		
		List<Integer[]> nn = new ArrayList<Integer[]>();
		
		for (Integer v_s : V) {
			for (Integer v_t : V) {
				nn.add(new Integer[] {v_s, v_t});
			}
		}
		
		for (int i = 0; i < nn.size(); i++) {
			if (nn.get(i)[0] == nn.get(i)[1]) {
				nn.remove(i);
			}
		}
		
		P = new Path[D.length*k];
		
		int i2 = 0;
		for (int i = 0; i < D.length; i++) {
			for (int j = 0; j < k; j++) {
				P[i2] = new Path(D[i], paths.returnPathAsIntegerTable(nn.get(i)[0], nn.get(i)[1], j+1));
				i2++;
			}
		}
		
		System.out.println("");
		
	}
	
	private void readExampleFromFile(String _file_name) {	
		try {
			FileReader input = new FileReader(_file_name);
			BufferedReader bufRead = new BufferedReader(input);
			
			String line;
			
			line = bufRead.readLine();
			while (line != null) {
				if(line.trim().equals("")) {
					line = bufRead.readLine();
					continue;
				}
				
				if (line.equalsIgnoreCase("D")) {
					line = bufRead.readLine();
					D = new Integer[Integer.valueOf(line)];
					for (int i = 0; i < D.length; i++) {
						D[i] = i+1;
					}
				}
				
				if (line.equalsIgnoreCase("V")) {
					line = bufRead.readLine();
					V = new Integer[Integer.valueOf(line)];
					for (int i = 0; i < V.length; i++) {
						V[i] = i+1;
					}
				}
				
				if (line.equalsIgnoreCase("T")) {
					T_v = new Integer[V.length];
					for (int i = 0; i < V.length; i++) {
						line = bufRead.readLine();
						T_v[i] = Integer.valueOf(line);
					}
				}
				
				if (line.equalsIgnoreCase("R")) {
					R_v = new Integer[V.length];
					for (int i = 0; i < V.length; i++) {
						line = bufRead.readLine();
						R_v[i] = Integer.valueOf(line);
					}
				}
				
				if (line.equalsIgnoreCase("PC")) {
					pc_v = new Double[V.length];
					for (int i = 0; i < V.length; i++) {
						line = bufRead.readLine();
						pc_v[i] = Double.valueOf(line);
					}
				}
				
				if (line.equalsIgnoreCase("K")) {
					line = bufRead.readLine();
					k = Integer.valueOf(line);
				}
				
				if (line.equalsIgnoreCase("GRAPH")) {
					line = bufRead.readLine();
					while (!line.trim().equals("")) {
						String[] splited = line.split("\\s+");
						paths.addAdjacency(Integer.valueOf(splited[0]),
								Integer.valueOf(splited[1]));
						line = bufRead.readLine();
						if (line == null) {
							break;
						}
					}
					continue;					
				}
				
				line = bufRead.readLine();
				
			}
			bufRead.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}








