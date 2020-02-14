
import java.util.ArrayList;
import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class TestSelector {

String directoryToSearch;
	/**
	* Constructor. 
	*/
	public TestSelector(String directoryToSearch){
		this.directoryToSearch = directoryToSearch;
	}
	
	/**
	* Methods
	*/
	
	/**
	* @Return: returns a list of paths to the tests to run?
	*/
	//The graph attained from the StaticRTSsolution class is the unedited dependency graph of all classes
	public ArrayList<String> selectTestsToRun(ArrayList<String> listOfChangedClasses, Graph dependencyGraph) {
		File file = new File(directoryToSearch);
		ArrayList<File> appendList = new ArrayList<File>();
		ArrayList<File> listOfTestFiles = new ArrayList<File>(listTestFilesForFolder(file, appendList));
		System.out.println("Total number of tests found: " + listOfTestFiles.size()); //used for measurements of safety and precision
		
		ArrayList<String> pathsToAffectedTests = new ArrayList<String>();
		int i = 0;
		for (String changedClass : listOfChangedClasses) {
			String[] nodeParts = changedClass.split("\\.");
			String nodeName = nodeParts[0];
			
			Node node = dependencyGraph.getNode(nodeName);
			
			if (node == null) {
				i++;
				//System.out.println("This many fuckups: " + i);
				continue; //if node is null there is a mismatch of the database and the way changed classes are found...?
			}
			
			ArrayList<String> servants = new ArrayList<String>();
			if (node.getServants().size() > 0) {
				servants = node.getServants();
			}
			ArrayList<Node> neighbours = new ArrayList<Node>();
			if (node.getNeighbours().size() > 0) {
				neighbours = node.getNeighbours();
			}
			ArrayList<Node> neighboursOfNeighbours = new ArrayList<Node>();
			ArrayList<String> servantsOfNeighbours = new ArrayList<String>();
			for (Node neighbour : neighbours) {
				neighboursOfNeighbours = neighbour.getNeighbours();
				servantsOfNeighbours = neighbour.getServants();
			}
			int immediateNodes = neighbours.size() + servants.size();
			int oneAwayNodes = neighboursOfNeighbours.size() + servantsOfNeighbours.size();
			
			for (Node neighbour : neighbours) {
				String className = neighbour.getName();
				String pathName = doesStringContainSubstring(listOfTestFiles, className); //Weird mix of boolean and saving a String value...
				if (!pathName.equals("") && !pathsToAffectedTests.contains(pathName)) {
					pathsToAffectedTests.add(pathName); //adds the path (i.e. string representation of the path) to a list
				}
			}
			for (String servant : servants) {
				String className = servant;
				String pathName = doesStringContainSubstring(listOfTestFiles, className); //Weird mix of boolean and saving a String value...
				if (!pathName.equals("") && !pathsToAffectedTests.contains(pathName)) {
					pathsToAffectedTests.add(pathName); //adds the path (i.e. string representation of the path) to a list
				}
			}
			for (Node neighbour : neighboursOfNeighbours) {
				String className = neighbour.getName();
				String pathName = doesStringContainSubstring(listOfTestFiles, className); //Weird mix of boolean and saving a String value...
				if (!pathName.equals("") && !pathsToAffectedTests.contains(pathName)) {
					pathsToAffectedTests.add(pathName); //adds the path (i.e. string representation of the path) to a list
				}
			}
			for (String servant : servantsOfNeighbours) {
				String className = servant;
				String pathName = doesStringContainSubstring(listOfTestFiles, className); //Weird mix of boolean and saving a String value...
				if (!pathName.equals("") && !pathsToAffectedTests.contains(pathName)) {
					pathsToAffectedTests.add(pathName); //adds the path (i.e. string representation of the path) to a list
				}
			}
		}
		System.out.println("pathsToAffectedTests.size(): " + pathsToAffectedTests.size());//DELET
		return pathsToAffectedTests;
	}
	
	
	/**
	* Help methods
	*/
	//A method which takes in a list of files (i.e. file locations) and turns them into string, and then compares them to the substring input value
	//@return: Returns the path to the test file if the input substring is a match for that test file
	private String doesStringContainSubstring(ArrayList<File> fileNames, String substring) {
		
		//the input substring is on format x.y.z.<classname>, needs to be split to get only <classname>
		String[] fixList = substring.split("\\.");
		int indexOfClassName = fixList.length - 1;
		String className = fixList[indexOfClassName];
		
		for (File file : fileNames) {
			String pathName = file.getPath();
			String[] pathParts = pathName.split("/");
			for (String part : pathParts) {
				if (part.equals(className)) {
					return pathName;
				}
			}
		}
		for (File file : fileNames) {
			String pathName = file.getPath();
			if (pathName.contains(className)) {
				return pathName;
			}
		}
		return "";
	}
	
	private ArrayList<File> listTestFilesForFolder(final File folder, ArrayList<File> appendList) {
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				listTestFilesForFolder(fileEntry, appendList);
			} else if (fileEntry.getName().contains("Test") && fileEntry.getName().toLowerCase().endsWith(".class")) {
				appendList.add(fileEntry);
			}
		}
		return appendList;
	}
}