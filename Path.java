package pstkm;

public class Path {
	
	static Integer count = 1;
	
	Integer n;		//	Numer porzadkowy sciezki
	Integer d;		//	Zapotrzeowanie, do którego należy ta ścieżka
	Integer [] V;	//	Zbior wezlow przez ktore przechodzi sciezka
	
	public Path(Integer _d, Integer [] _V) {
		this.n = count;
		V = new Integer [_V.length];
		for (int i = 0; i < _V.length; i++) {
			V[i] = _V[i];
		}
		this.d = _d;
		count++;
	}
	
	public Integer getNr() {
		return this.n;
	}
	
	public Integer getD() {
		return this.d;
	}
	
	public Integer getStartV() {
		return this.V[0];
	}
	
	public Integer getEndV() {
		return this.V[V.length-1];
	}
	
	public Integer[] getAllV() {
		return this.V;
	}

}
