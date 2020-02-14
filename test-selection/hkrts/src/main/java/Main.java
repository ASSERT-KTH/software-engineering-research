import java.io.File;
import java.util.ArrayList;

//The command to run HKRTS locally is
// java StaticRTSsolution /cygdrive/c/projects/HKRTS/rp/HKRTS/src/main/java/dot C:/projects/commons-cli/rp/commons-cli

class Main {	
	//Make the path to the java code this: C:\projects\HKRTS\rp\HKRTS\src\main\java
	
	public Main() {

	}
	
	/**
	* @Args the first argument is a String representing the dot file output location,
	* the second is a String representing the file or directory that jdeps will start working on
	*/
	public static void main (String[] args) {
		long totalTimeStart = System.currentTimeMillis();
		
		//prep the input arguments
		String dotOutputLocation = "/cygdrive/c/projects/HKRTS/rp/HKRTS/src/main/java/dot"; 
		//change targetProjectLocation when running on a new project
		String targetProjectLocation = "C:/projects/commons-cli/rp/commons-cli";

		
		try {
			DependencyFinder dFinder = new DependencyFinder();
			dFinder.jdepsLibsRecursive(dotOutputLocation, targetProjectLocation);
		} catch (Exception e) {
			e.printStackTrace();
		}
				
		DependencyGraph dGraph = new DependencyGraph();
		Graph dependencyGraph = new Graph(dGraph.createGraphData(dotOutputLocation));
		
		ChecksumController checksumController = new ChecksumController();
		ArrayList<String> listOfChangedClasses = new ArrayList<String>();
		listOfChangedClasses = checksumController.getChangedChecksums(targetProjectLocation);

		TestSelector tSelect = new TestSelector(targetProjectLocation);
		ArrayList<String> pathsToTestsToRun = new ArrayList<String>(tSelect.selectTestsToRun(listOfChangedClasses, dependencyGraph));
		
		MavenInvokerRunner mvnInvoRunner = new MavenInvokerRunner();
		mvnInvoRunner.runCommand("dependency:build-classpath", new File("C:/projects/HKRTS/rp/HKRTS"));
		File originDirectory = new File(targetProjectLocation);
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