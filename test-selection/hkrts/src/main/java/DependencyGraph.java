

import java.util.ArrayList;
import java.io.*;
import java.util.Map;
import java.util.HashMap;

public class DependencyGraph { //Check for maybe cycle graphs


	/**
	* Constructor. 
	*/
	public DependencyGraph(){
		
	}
	
	/**
	* Methods
	*/

	/**
	* @Return: This method should return a graph
	*/
	public Graph createGraphData (String dotOutputLocation) {
		DotInterpreter dInterp = new DotInterpreter(dotOutputLocation);
		Map<String, ArrayList<String>> dependencyMap = new HashMap<String, ArrayList<String>>();
		dependencyMap = dInterp.getDotFilesAsMap();
		
		Graph dependencyGraph = turnMapIntoGraph(dependencyMap);
		 
		//Each key-value pair is a directed edge from the key node to the value node
		
		return dependencyGraph;
	}
	
	/**
	* Help methods
	*/
	//Extract all nodes from the map. Each key is a node and each value is a node. The values gotten from each key is a directed edge from key to value.
	//A Graph is a list of Nodes. A Node is a String name and a list of Nodes neighbours.
	private Graph turnMapIntoGraph (Map<String, ArrayList<String>> dependencyMap) {
		Graph dGraph = new Graph();
		
		for (Object key : dependencyMap.keySet()) {
			Node node = new Node(key.toString());
			dGraph.addNode(node);
		}
		
		for (Node node : dGraph.getNodes()) {
			ArrayList<String> nieghboursAndServants = dependencyMap.get(node.getName());
			for (String name : nieghboursAndServants) {
				if (dGraph.checkNodeDFS(name)) {
					node.addNeighbour(dGraph.getNode(name));
				} else {
					node.addServant(name);
				}
			}
		}
		return dGraph;
	}
}