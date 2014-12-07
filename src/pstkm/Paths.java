package pstkm;

import edu.asu.emit.qyan.alg.control.YenTopKShortestPathsAlg;
import edu.asu.emit.qyan.alg.model.Graph;
import edu.asu.emit.qyan.alg.model.Path;
import edu.asu.emit.qyan.alg.model.VariableGraph;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


/**API do algorytmu generacji k najkrótszych œcie¿ek:
 * link do metody: https://www.google.pl/search?q=A+New+Implementation+Of+Yen%27s+Ranking+Loopless+Paths+Algorithm
 * U¿ycie API:
   //Trivial-1
 * Paths paths = new Paths();
 * paths.addAdjacency(1, 2);
   paths.addAdjacency(2, 3);
   paths.addAdjacency(2, 3);
   paths.modelReady();
   paths.printPathsBetweenVertexes(1, 3, 10);
   //Non-Trivial1
   Paths paths = new Paths();
   paths.addAdjacency(1, 2);
   paths.addAdjacency(1, 3);
   paths.addAdjacency(1, 4);
   paths.addAdjacency(2, 3);
   paths.addAdjacency(3, 4);
   paths.modelReady();
   paths.printPathsBetweenVertexes(1, 3, 10); lub paths.returnPathAsIntegerTable(1, 3, 2);
   //Non-Trivial2
   paths.addAdjacency(1, 2);
   paths.addAdjacency(2, 3);
   paths.addAdjacency(3, 4);
   paths.addAdjacency(4, 1);
   paths.modelReady();
   paths.printnPathsBetweenVertexes(1, 3, 10); lub paths.returnPathAsIntegerTable(1, 3, 2);
 */
public class Paths {
	
	YenTopKShortestPathsAlg yenAlg;
	Graph graph;
	List<Path> shortest_paths_list;
	Vector<int[]> vertexesMap;
	
	public Paths()
	{	
		vertexesMap = new Vector<int[]>();
	}
	
	private void generatePaths(int start, int end, int k)
	{
		shortest_paths_list = yenAlg.get_shortest_paths(graph.get_vertex(start-1), graph.get_vertex(end-1), k);		
	}
	
	
	/**Zwraca Œcie¿ki miêdzy wêz³ami
	 * */
	public Vector<Vector<Integer>> returnPathsBetweenVertexes (int start,int end, int k)
	{
		generatePaths(start,end,k);
		return convert();
	}
	
	/**Zwraca œcie¿kê o okreœlonym przez number indeksie miêdzy wêz³ami
	 * Indeksowanie number od 0!!
	 * */
	public Integer[] returnPathAsIntegerTable(int start, int end, int number)
	{
		Vector<Vector<Integer>> temporaryContainer = returnPathsBetweenVertexes (start,end, 1000);
		Integer[] pth = new Integer[temporaryContainer.get(number-1).size()];
		for (int i=0;i<pth.length;i++)
		{
			pth[i]=temporaryContainer.get(number-1).get(i);
		}
		return pth;
	}
	
	private Vector<Vector<Integer>> convert()
	{
		Vector<Vector <Integer> > paths = new Vector<Vector <Integer> >(5);
		int i=0;
		for (Path path : shortest_paths_list)
		{
			paths.add(new Vector<Integer>());
			for (Iterator<edu.asu.emit.qyan.alg.model.abstracts.BaseVertex> iter = path.get_vertices().iterator();iter.hasNext();)
			{
				paths.get(i).add(iter.next().get_id()+1);
			}
			i++;
		}
		return paths;
	}
	
	/**
	 * */
	public void printPathsBetweenVertexes(int start, int end, int k)
	{
		shortest_paths_list = yenAlg.get_shortest_paths(graph.get_vertex(start-1), graph.get_vertex(end-1),100);
		System.out.println(returnPathsBetweenVertexes(start,end,k));
	}
	
	/**
	 * Iloœæ œcie¿ek miêdzy wêz³ami*/
	public int pathCountBetweenVertexes(int start, int end)
	{
		shortest_paths_list = yenAlg.get_shortest_paths(graph.get_vertex(start-1), graph.get_vertex(end-1),100);
		return yenAlg.get_result_list().size();
	}
	
	/**Dodanie œcie¿ki miêdzy wêz³ami o tych numerach
	 * */
	public void addAdjacency(int start, int end)
	{
		int[] temp = new int[2];
		temp[0]=start-1;
		temp[1]=end-1;
		vertexesMap.add(temp);
	}
	
	public void modelReady()
	{		
			try {
				PrintWriter print_line = new PrintWriter(new FileWriter( "model.txt" , false));
				print_line.println(vertexesMap.size());
				print_line.println();
				for (Iterator<int[]> iter = vertexesMap.iterator();iter.hasNext();)
				{
					int[] temp = iter.next();
					print_line.println(temp[0]+" "+temp[1]+" 1");
					print_line.println(temp[1]+" "+temp[0]+" 1");
				}
				print_line.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		//Zapisz do pliku w odpowiednim formacie i uruchom procedurê liczenia œcie¿ek
		String graphdirectory = "model.txt";
		graph = new VariableGraph(graphdirectory);
		yenAlg = new YenTopKShortestPathsAlg(graph);
	}
}


