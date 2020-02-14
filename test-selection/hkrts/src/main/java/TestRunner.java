import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.Runtime;
import java.lang.Class;


public class TestRunner {
	Runtime rt;
	Process pr;

	//Constructor
	public TestRunner() {

	}

	public int runTests(ArrayList<String> listOfTestsToRun, MavenInvokerRunner mvnInvo) {//, String originPath
		int numberOfTestsRun = 0;
		int errors = 0;
		System.out.println("Scheduled to run " + listOfTestsToRun.size() + " tests.");
		ArrayList<String> classNamesToRun = new ArrayList<String>();

		for (String path : listOfTestsToRun) {
			String[] pathParts = path.split("\\\\");

			//classpath
			StringBuilder sBuilder = new StringBuilder();
			for (int i = 0; i < pathParts.length - 1; i++) {
				if (i == pathParts.length - 2) { //dont add a / to the last one
					sBuilder.append(pathParts[i]);
				} else {
					sBuilder.append(pathParts[i]);
					sBuilder.append("/");
				}
			}
			String modPath = sBuilder.toString();

			//class name
			int index = pathParts.length - 1;
			String className = pathParts[index];
			String[] classNameParts = className.split("\\.");
			String classNameToRun = classNameParts[0];
			classNamesToRun.add(classNameToRun);
		}

		StringBuilder mavenInputBuilder = new StringBuilder();
		for (int i = 0; i<classNamesToRun.size(); i++) {
			mavenInputBuilder.append(classNamesToRun.get(i));
			if (i < (classNamesToRun.size() - 1)) { //If there are more names to add after this one
				mavenInputBuilder.append(",");
			}
		}
		if (executeMavenCommand(mavenInputBuilder.toString(), mvnInvo)) {
			return classNamesToRun.size();
		} else {
			return -1;
		}
	}

	//Try give this a list of classes to run??
	private boolean executeMavenCommand(String className, MavenInvokerRunner mvnInvoRunner) {//String path, 
		// this should be "-Dtest=" + a really long string of all test names
		String runTimeArgument = "-Dtest=" + className + " surefire:test -DfailIfNoTests=false";
		try {
			mvnInvoRunner.runQuickCommand(runTimeArgument);//, file
			return true;
		} catch(Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
			return false;
		}
	}

}