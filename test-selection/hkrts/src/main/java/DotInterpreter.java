
import java.io.*;
import java.util.ArrayList;
import java.util.AbstractCollection.*;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;

public class DotInterpreter {

	ArrayList<String> lines = new ArrayList<String>();
	String dotOutputLocation = "";

	/**
	 * Constructor. 
	 */
	public DotInterpreter(String dotOutputLocation){
		this.dotOutputLocation = dotOutputLocation;
	}

	/**
	 * Methods
	 */
	//@Return: returns a map of all dependencies in all files in the dot directory
	//This works since the dependency is always noted the same way: [the class]->[depends on this]
	//This means that the map will contain one key per class, and all values for that key is the deps
	public Map<String, ArrayList<String>> getDotFilesAsMap() {
		readDotFiles();
		Map<String, ArrayList<String>> dependencyMap = new HashMap<String, ArrayList<String>>();
		dependencyMap = splitLineAndPutMap();
		return dependencyMap;
	}

	private void readDotFiles () {
		File dotoutput = new File(dotOutputLocation);
		ArrayList<File> dotFiles = new ArrayList<File>();
		dotFiles = listFilesForFolder(dotoutput);
		// At this point we have a list of file names, the full path of the list is "dotOutputLocation/<filename>"

		FileReader fReader;
		for (File dotFile : dotFiles) {
			try {
				fReader = new FileReader(dotFile);
				BufferedReader bReader = new BufferedReader(fReader);

				//This is temporary
				while (bReader.ready()) {
					String line = bReader.readLine();
					if (!line.equals("}")) {
						lines.add(line);
					}
				}

				bReader.close();
				fReader.close();
			} catch(Exception e) {
				System.out.println(e.toString());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Help methods
	 */
	private Map<String, ArrayList<String>> splitLineAndPutMap() { //TODO this method is still not adding neighbours for LogstashAccessFormatter even though it is the dependency 4 times
		Map<String, ArrayList<String>> dependencyMap = new HashMap<String, ArrayList<String>>();
		int i = 0;
		for (String rawLine : lines) {
			ArrayList<String> parts = new ArrayList<String>(patternExtractor(rawLine));

			String dependor = "";
			String dependency = "";

			if (parts.size() > 1) {
				dependor = parts.get(0);
				dependency = parts.get(1);
			} else if (parts.size() == 1) {
				dependor = parts.get(0);
			}

			ArrayList<String> arList;
			if (!dependencyMap.containsKey(dependency)) { //if no, add key + initialize list		
				arList = new ArrayList<String>();
				arList.add(dependor);
				dependencyMap.put(dependency, arList);
			} else if (!dependency.equals("")) { //if yes, add dependor to the list
				arList = dependencyMap.get(dependency);
				if (!arList.contains(dependor)) { //dont add same neighbour multiple times
					arList.add(dependor);
				}
				dependencyMap.replace(dependency, arList);
			}
		}
		return dependencyMap;
	}

	private ArrayList<String> patternExtractor (String rawLine) {
		ArrayList<String> returnList = new ArrayList<String>();
		String strong = "";

		Pattern dotPattern = Pattern.compile("\\w+[\\S+|\\s+]+");
		for (String substring : rawLine.split("\"")) {
			Matcher m = dotPattern.matcher(substring);
			while (m.find()) {
				strong = m.group();
				if (strong.contains(" ")) {
					String[] stronglist = strong.split(" ");
					String nameWithoutComment = stronglist[0];
					returnList.add(nameWithoutComment);
				} else {
					returnList.add(strong);
				}
			}
		}
		return returnList;
	}

	private ArrayList<File> listFilesForFolder(final File folder) {
		ArrayList<File> fileNames = new ArrayList<File>();
		for (final File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				listFilesForFolder(fileEntry);
			} else {
				if (!fileEntry.getPath().contains("java.") && !fileEntry.getPath().contains("sun.")) { //filter
					fileNames.add(fileEntry);
				}
			}
		}
		return fileNames;
	}

}