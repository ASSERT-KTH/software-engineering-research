import java.io.File;
import java.util.Collections;
 
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
 
public class MavenInvokerRunner {
	InvocationRequest request;
	Invoker invoker;
	final StringBuilder mavenOutput;
    //public static void main(String[] args) { //OwO wtf is this?
    //    new MavenInvokerRunner().runCommand("dependency:build-classpath",
    //            new File("C:/projects/HKRTS/rp/HKRTS/src/main/java"));
    //}
	
	public MavenInvokerRunner() {
		request = new DefaultInvocationRequest();
		invoker = new DefaultInvoker();
		mavenOutput = new StringBuilder();
        invoker.setOutputHandler(new InvocationOutputHandler() {
            public void consumeLine(String line) {
                mavenOutput.append(line).append(System.lineSeparator());
            }
        });
	}
 
    public void runCommand(String mavenCommand, File workingDirectory) {
        request.setPomFile(new File(workingDirectory, "pom.xml"));
        request.setGoals(Collections.singletonList(mavenCommand));
 
        invoker.setMavenHome(new File("C:/Maven/apache-maven-3.5.3"));
        try {
            InvocationResult invocationResult = invoker.execute(request);
            // Process maven output
            System.out.println(mavenOutput);
            if (invocationResult.getExitCode() != 0) {
                // handle error
            }
        } catch (MavenInvocationException e) {
            e.printStackTrace();
        }
    }
	
	public void prepForQuickCommands(File workingDirectory) {
		request.setPomFile(new File(workingDirectory, "pom.xml"));
	}
	
	public void runQuickCommand (String mavenCommand) {
		request.setGoals(Collections.singletonList(mavenCommand));
		try {
            InvocationResult invocationResult = invoker.execute(request);
            // Process maven output
            System.out.println(mavenOutput);
            if (invocationResult.getExitCode() != 0) {
                // handle error
            }
        } catch (MavenInvocationException e) {
            e.printStackTrace();
        }
	}
 
}