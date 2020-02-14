import java.util.ArrayList;

public class Graph {
	ArrayList<Node> nodes;
	
	//empty constructor
	public Graph() {
		nodes = new ArrayList<Node>();
	}
	//copycontructor
	public Graph(Graph graph) {
		this.nodes = graph.getNodes();
	}
	
	//A method to add nodes
	public void addNode(Node node) {
		nodes.add(node);
	}
	
	//a method which returns the Graph (i.e. the list of nodes)
	public ArrayList<Node> getNodes() {
		return nodes;
	}
	
	//TODO Dude this is not as simple as "go get it", you need to choose and implement a graph traversal algorithm. Maybe both so you can switch and see which one is best.
	public Node getNode(String className) { //TODO returns a "match" but may return a node with the same name but not same object
		int index = -1;
		
		for (Node node : nodes) {
			String[] workStrings = node.getName().split("\\.");
			for (String workString : workStrings) {
				String[] stringList = workString.split("\""); //shave the quotes off
				String namePart = stringList[0]; //always only 1 elem
				if (namePart.equals(className)) {
					index = nodes.indexOf(node);
				}
			}
			if (index == -1 && node.getName().equals(className)) {
				index = nodes.indexOf(node);
			}
			if (index > -1) {
				return nodes.get(index);
			}
		}
		return null;
	}
	
	public Node getNode(Node node) {
		int index = nodes.indexOf(node);
		return nodes.get(index);
	}
	
	public Node getNodeByIndex(int index) {
		return nodes.get(index);
	}
	
	public int getIndex(Node node) {
		return nodes.indexOf(node);
	}
	
	public boolean nodeCheck(Node node) {
		return nodes.contains(node);
	}
	
	public boolean checkNodeDFS(String nodeName) { //TODO okay here is where you need to implement your graph traversal algorithm. just doing a foreach on ArrayList == BFS algorithm?
		return helpForDFS(nodeName, nodes);
	}
	
	private boolean helpForDFS(String nodeName, ArrayList<Node> recursiveNodes) { //kan behöva ta ett argument för rekursiv medåkning.
		for (Node node : recursiveNodes) {
			if (node.getNeighbours().size() > 0) {
				helpForDFS(nodeName, node.getNeighbours());
			} else if (node.getName().equals(nodeName)) {
				return true;
			} else {
				return false;
			}
		}
		System.out.println("Im tired but this should never be reached right?");
		return false;
	}
	
	public int size() {
		return nodes.size();
	}
}