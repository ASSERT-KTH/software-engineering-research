import java.util.ArrayList;

public class Node {
    String name;
    ArrayList<Node> neighbours;
	ArrayList<String> servants;
	
	//Constructor without neighbours
	public Node (String name) {
		this.name = name;
		neighbours = new ArrayList<Node>();
		servants = new ArrayList<String>();
	}
	//Constructor with neighbours
	public Node (String name, ArrayList<Node> neighbours) {
		this.name = name;
		this.neighbours = neighbours;
		servants = new ArrayList<String>();
	}
	
	public void addNeighbour(Node node) {
		neighbours.add(node);
	}
	
	public void addServant (String servantName) {
		servants.add(servantName);
	}
	
	public void addNeighbours(ArrayList<Node> nodeList) {
		for (Node node : nodeList) {
			neighbours.add(node);
		}
	}
	
	public ArrayList<Node> getNeighbours() {
		return neighbours;
	}
	
	public ArrayList<String> getServants() {
		return servants;
	}
	
	public String getName() {
		return name;
	}
	
	public int size() {
		return neighbours.size();
	}
	
	//HALP MATHODS
	private void trimNeighbours() {
		ArrayList<Integer> indexList = new ArrayList<Integer>();
		for (Node neighbour : neighbours) {
			if (neighbour.getName().equals("")) {
				indexList.add(0, neighbours.indexOf(neighbour)); //Adding at index 0 makes the final list reversed
			}
		}
		for (int i : indexList) {
			neighbours.remove(i);
		}
	}
}