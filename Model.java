package pstkm;

import ilog.concert.*;
import ilog.cplex.*;

public class Model {
	
	// Temp, pozniej beda wczytywane dane wejsciowe
	int d_len = 6;
	int v_len = 3;
	int p_len = 12;
	
	
	//	Zbiory
	Integer [] D = new Integer[d_len];	//	Zbior zapotrzebowan
	Integer [] V = new Integer[v_len];	//	Zbior wezlow
	Path [] P = new Path[p_len];		//	Zbior sciezek
	
	
	// Wartosci stale
	Integer [][][] delta_vdp = new Integer[V.length][D.length][P.length];	// [v][d][p], {0,1}	=1-jezeli wezel v lezy na sciezce p zapotrzebowania d,=0-w innym przyp
	Integer [][] s_vd = new Integer[V.length][D.length];			// [v][d], {0,1} =0 –wezel v jest wezlem startowym dla zapotrzebowania d,=1-w innym przyp
	Integer [][] t_vd = new Integer[V.length][D.length];			// [v][d], {0,1} =0 –wezel v jest wezlem koncowym dla zapotrzebowania d,=1-w innym przyp
	Integer [] T_v = new Integer[V.length];				// [v], maksymalna szybkosc z jaka moze wysylac dane wezel, T_v >= 0
	Integer [] R_v = new Integer[V.length];				// [v], maksymalna szybkosc z jaka moze odbierac dane wezel, R_v >= 0
	Double [] pc_v = new Double[V.length];			// [v], wsp. zuzycia energii wezla v
	///TODO zdefiniowac r
	
	
	public Model() {
		startData();
		fill_delta_s_t();	
		model();
	}
	
	public void model() {
		Double result = null;
		try {
			IloCplex cplex = new IloCplex();
			
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
						//System.out.println("v: "+v+", d: "+d+", p: "+p.getNr()+", temp: "+temp);
						if (temp != 0) {
							if (x_dp[d-1][p.getNr()-1] != null) {
								lhs.addTerm(temp, x_dp[d - 1][p.getNr() - 1]);
							}
						}
					}
				}
				cplex.addLe(lhs, T_v[v-1]);
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
				Double d = ((double)T_v[v-1] + (double)R_v[v-1]) * pc_v[v-1];
				Integer rhs = d.intValue();
				cplex.addLe(lhs, rhs);
			}
			
			if (cplex.solve()) {
				System.out.println("obj = "+cplex.getObjValue());
				result = cplex.getObjValue();
			}
			else {
				System.out.println("Model not solved");
				result = null;
			}
			
		} 
		catch (IloException exc) {
			exc.printStackTrace();
		}
	}
	
	
	private void fill_delta_s_t() {
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
	
	private void startData() {
		for (int i = 0; i < v_len; i++) {
			V[i] = i+1;
		}
		for (int i = 0; i < d_len; i++) {
			D[i] = i+1;
		}
		
		P[0] = new Path(1, new Integer[] {1, 2});
		P[1] = new Path(1, new Integer[] {1, 3, 2});
		P[2] = new Path(2, new Integer[] {1, 3});
		P[3] = new Path(2, new Integer[] {1, 2, 3});
		P[4] = new Path(3, new Integer[] {2, 1});
		P[5] = new Path(3, new Integer[] {2, 3, 1});
		P[6] = new Path(4, new Integer[] {2, 3});
		P[7] = new Path(4, new Integer[] {2, 1, 3});
		P[8] = new Path(5, new Integer[] {3, 1});
		P[9] = new Path(5, new Integer[] {3, 2, 1});
		P[10] = new Path(6, new Integer[] {3, 2});
		P[11] = new Path(6, new Integer[] {3, 1, 2});
		
		T_v[0] = 60;
		T_v[1] = 60;
		T_v[2] = 50;
		
		R_v[0] = 60;
		R_v[1] = 40;
		R_v[2] = 40;
		
		pc_v[0] = 0.5;
		pc_v[1] = 0.8;
		pc_v[2] = 0.5;
	}

}
