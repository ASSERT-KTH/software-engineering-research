
import java.io.*;

public class DependencyFinder {
	
	Runtime rt;
	Process pr;
	
	/**
	* Constructor. 
	*/
	public DependencyFinder(){
		rt = Runtime.getRuntime();

	}
	
	/**
	* Methods
	*/
	
	public void jdepsLibsRecursive (String dotOutputLocation, String fileOrDirectoryLocation) throws IOException {
		//-classpath 'libs/'  jdeps 
		String runTimeArgument = "jdeps -verbose -recursive -dotoutput " + dotOutputLocation + " " + fileOrDirectoryLocation;
		try {
			rt = Runtime.getRuntime();
			pr = rt.exec(runTimeArgument);
			int exitVal = pr.waitFor();
			if (exitVal == 0) {
				System.out.println("DependencyFinder: Error code " + exitVal + ", operation completed successfully.");
			} else {
				System.out.println("DependencyFinder: Exited with error code "+exitVal);
			}
		} catch(Exception e) {
			System.out.println(e.toString());
            e.printStackTrace();
        }
		
	}
	
}