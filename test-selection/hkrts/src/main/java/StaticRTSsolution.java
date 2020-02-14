import java.io.File;
import java.util.ArrayList;

//The command to run HKRTS locally is
// java StaticRTSsolution /cygdrive/c/projects/HKRTS/rp/HKRTS/src/main/java/dot C:/projects/commons-cli/rp/commons-cli
class StaticRTSsolution {
		
	/**
	* STARTS has this process;
	* (i) find dependencies among types in the application, (JDEPS) (DependencyFinder) - CHECK
	* (ii) construct the TDG, (dependencyGraph) - 
	* (iii) find the changed types between two revisions of the application, (maybe also checksumController?) - CHECK checksumController returns the list of all changed classes
	* (iv) store checksums of all types from the current revision, (checksumController) - CHECK
	* (v) select the tests impacted by the changed types, and (testSelector)
	* (vi) run the impacted tests. (to be done here or in another dedicated class?)
	*/
	// /cygdrive/c/Maven/apache-maven-3.5.3/boot
	
	//Make the path to the java code this: C:\projects\HKRTS\rp\HKRTS\src\main\java
	
	//remember to check for size of TE project (amount of classes); if the size of the graph is not too big the performance is neglectable(?).
	//check checksum for if comments change checksum
	public StaticRTSsolution() {

	}
	
	/**
	* @Args the first argument is a String representing the dot file output location,
	* the second is a String representing the file or directory that jdeps will start working on
	*/
	public static void main (String[] args) {
		long totalTimeStart = System.currentTimeMillis();
		//prep the input arguments
		ArrayList<String> argList = new ArrayList<String>();
		for(int i = 0; i < args.length; i++) {
            argList.add(args[i]);
        }
		
		try {
			DependencyFinder dFinder = new DependencyFinder();
			dFinder.jdepsLibsRecursive(argList.get(0), argList.get(1));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//TODO: structure the graph rework DO THIS AFTER THE DEBUG WORK IS DONE
		//MIGHT NOT HAVE TO DO THIS if HKRTS works good after debug.
		//The rework might also take longer if we do multiple chekcups for dependencies
		/**
		//At this point in the program the dot output has done its thing and found the relations
		ChecksumController checksumController = new ChecksumController();
		ArrayList<String> listOfChangedClasses = new ArrayList<String>();
		listOfChangedClasses = checksumController.getChangedChecksums(argList.get(1));
		
		for Class in listOfChangedClasses
			find all neighbours
				if Class, add as neigh
				else, add as servant
		*/
				
		DependencyGraph dGraph = new DependencyGraph();
		Graph dependencyGraph = new Graph(dGraph.createGraphData(argList.get(0)));
		//System.out.println("graph size: " + dependencyGraph.size());
		
		ChecksumController checksumController = new ChecksumController();
		ArrayList<String> listOfChangedClasses = new ArrayList<String>();
		listOfChangedClasses = checksumController.getChangedChecksums(argList.get(1));

		//System.out.println("listOfChangedClasses.size(): " + listOfChangedClasses.size());
		TestSelector tSelect = new TestSelector(argList.get(1));
		ArrayList<String> pathsToTestsToRun = new ArrayList<String>(tSelect.selectTestsToRun(listOfChangedClasses, dependencyGraph));
		
		MavenInvokerRunner mvnInvoRunner = new MavenInvokerRunner();
		mvnInvoRunner.runCommand("dependency:build-classpath", new File("C:/projects/HKRTS/rp/HKRTS"));
		File originDirectory = new File(argList.get(1));
		mvnInvoRunner.prepForQuickCommands(originDirectory);
		TestRunner runnerOfTests = new TestRunner();
		long timeForBreakPoint = System.currentTimeMillis();
		int runTestsAndCountThem = runnerOfTests.runTests(pathsToTestsToRun, mvnInvoRunner); 
		
		long totalTimeEnd = System.currentTimeMillis();
		String timeElapsedFormatted = formatElapsedTime(totalTimeStart, totalTimeEnd);
		System.out.println("Run completed, tests run: " + runTestsAndCountThem);
		System.out.println("Total run time: " + timeElapsedFormatted);
		System.out.println("Time to find tests: " + formatElapsedTime(totalTimeStart, timeForBreakPoint) + ", time to run tests: " + formatElapsedTime(timeForBreakPoint, totalTimeEnd));
	}
	
	private static String formatElapsedTime(long tStart, long tEnd) {
		long tDelta = tEnd - tStart;
		double elapsedSeconds = tDelta / 1000.0;
		int elapsedMinutes = 0;
		int elapsedHours = 0;
		String formattedTime = "";
		
		while (elapsedSeconds > 60) {
			elapsedSeconds -= 60.0;
			elapsedMinutes++;
		}
		while (elapsedMinutes > 60) {
			elapsedMinutes -= 60;
			elapsedHours++;
		}
		
		if (elapsedHours > 0) {
			formattedTime = "" + elapsedHours + "h, " + elapsedMinutes + "m, " + elapsedSeconds + "s";
		} else if (elapsedMinutes > 0) {
			formattedTime = "" + elapsedMinutes + "m, " + elapsedSeconds + "s";
		} else {
			formattedTime = "" + elapsedSeconds + "s";
		}
		return formattedTime;
	}
	
	
}