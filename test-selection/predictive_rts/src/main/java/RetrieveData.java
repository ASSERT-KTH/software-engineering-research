import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class RetrieveData {

    private String packageName;
    private String dir_project;
    public RetrieveData(String project){
        dir_project = project;
    }
    
    //Find test reports in surefire-reports directory
    public List<File> findFiles(String rootDir){
        File dir = new File(rootDir+"/target/surefire-reports");
        final String[] SUFFIX = {"txt"};  // use the suffix to filter
        Collection<File> files = FileUtils.listFiles(dir, SUFFIX, true);
        List<File> listFiles = new ArrayList<>(files);
        return listFiles;
    }
    //Read all the files of the surefire-reports
    public List<List<String>> readFiles(List<File> listOfFiles) throws  IOException{
        List<List<String>> data = new ArrayList<>();
        for (File child : listOfFiles){

            String string = FileUtils.readFileToString(child);
            String testRuns = StringUtils.substringBetween(string,"Tests run:", ",");
            String failures = StringUtils.substringBetween(string,"Failures:", ",");
            String errors = StringUtils.substringBetween(string,"Errors:", ",");
            String skipped = StringUtils.substringBetween(string,"Skipped:", ",");
            String testSet = StringUtils.substringBetween(string,"Test set:", "\n");
            String testRunOutput = StringUtils.deleteWhitespace(testRuns);
            String failuresOutput = StringUtils.deleteWhitespace(failures);
            String errorsOutput = StringUtils.deleteWhitespace(errors);
            String skippedOutput = StringUtils.deleteWhitespace(skipped);
            String testSetOutput = StringUtils.deleteWhitespace(testSet);
            /*System.out.println("test runs: " + testRunOutput);
            System.out.println("Failures:  " + failuresOutput);
            System.out.println("Errors: " + errorsOutput);
            System.out.println("Skipped: " + skippedOutput);
            System.out.println("Test set: " + testSetOutput);*/
            List<String> tmp = Arrays.asList(testSetOutput,testRunOutput,failuresOutput
                    ,errorsOutput, skippedOutput);
            data.add(tmp);
        }
        return data;
    }
    //Read what the STARTS program output
    public Map<String,String[]> readIntermediateData(String path_to_dir) throws IOException{
        File dataToRead = new File(path_to_dir);
        String string = FileUtils.readFileToString(dataToRead);
        String[] rows = string.split("\n");
        Map<String,String[]> mp = new HashMap<>();
        for(int i = 0; i<rows.length;i++){
            String[] row = rows[i].split(",");
            if(row.length>8){
                mp.put(row[8],row); //Affected test
            }
        }
        return mp;
    }
    //combine all the data
    public static void combineData(List<List<String>> data,Map<String,String[]> startsData, int current_iteration,String project_dir) throws IOException{
        File allData = new File(project_dir+ "/erik-files/allData.txt");
        allData.createNewFile();
        for(int i = 0; i<data.size(); i++){
            List<String> oneElementData = data.get(i);

            String testSet = oneElementData.get(0);
            String testRun = oneElementData.get(1);
            String testFailures = oneElementData.get(2);
            String testErrors = oneElementData.get(3);
            String testSkipped = oneElementData.get(4);
            String[] oneStartsData = startsData.get(testSet);
            if(oneStartsData != null){
                oneStartsData[0] = testRun;
                oneStartsData[1] = testFailures;
                oneStartsData[2] = testErrors;
                oneStartsData[3] = testSkipped;
                String writeString = String.join(",",oneStartsData) + "," + Integer.toString(current_iteration);
                writeString = writeString + "\n";
                try{
                    Files.write(Paths.get(allData.getAbsolutePath()),writeString.getBytes(), StandardOpenOption.APPEND);
                }catch (IOException e)  {
                    e.printStackTrace();
            }
            }

        }
    }
    //Clean all tmpFiles from starts and test-reports
    public static void cleanTmpFiles(String dir_project) throws IOException{
        File testWrite = new File(dir_project + "/erik-files/testWrite.txt");
        File sureFireReports = new File(dir_project + "/target/surefire-reports");
        testWrite.delete();
        testWrite.createNewFile();
        FileUtils.cleanDirectory(sureFireReports);
    }
    //Hard reset of data, clean everything (Set to false by default)
    public static void cleanAllData(String dir_project) throws IOException{
        File testWrite = new File(dir_project + "/erik-files/testWrite.txt");
        File allData = new File(dir_project + "/erik-files/allData.txt");
        File sureFireReports = new File(dir_project + "/target/surefire-reports");
        File mutantNotVisited = new File(dir_project + "/erik-files/mutantsNotVisited.txt");
        mutantNotVisited.delete();
        mutantNotVisited.createNewFile();
        testWrite.delete();
        testWrite.createNewFile();
        allData.delete();
        allData.createNewFile();
        FileUtils.cleanDirectory(sureFireReports);
    }
    
    //Actually running the extraction
    public static void extractData(int current_iteration, String dir_to_project){
        /*Change this to true to clean all data*/
        boolean clean = false;

        String dir_to_data = dir_to_project+"/erik-files/testWrite.txt";
        try {
            if (clean){
                cleanAllData(dir_to_project);
            } else {
                RetrieveData ob = new RetrieveData(dir_to_project);
                List<File> listOfFiles = ob.findFiles(dir_to_project);
                //System.out.println("listOfPaths: " + listOfPaths[0]);

                List<List<String>> data_one = ob.readFiles(listOfFiles);
                Map<String, String[]> data_two = ob.readIntermediateData(dir_to_data);
                combineData(data_one, data_two,current_iteration,dir_to_project);
                cleanTmpFiles(dir_to_project);
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        //extractData(0);
        System.out.println("HEJ");
    }


}


/*
* 0 = TestRun
* 1 = TestFailures
* 2 = TestErrors
* 3 = TestSkipped
* 4 = File cardinality
* 5 = affected tests
* 6 = Minimal distance
* 7 = num connected tests
* 8 = what test failed
* 9-X = connected tests
* X+num of connected tests = changed made to file
* X+num of connected tests+1 = current iteration
* */

/* TODO the fourth column says failure sometimes (i guess if the test fails) */
