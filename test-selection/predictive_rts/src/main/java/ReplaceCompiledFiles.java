import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ReplaceCompiledFiles {

    public ReplaceCompiledFiles(){}

    public static void main (String[] args){
        //Values that you can change 
        int timeoutInSeconds = 200;
        int cheat_iteration = 0;
        int iterations = 10;
        int leastAmountOfFiles = 1;
        int mostAmountOfFiles = 10;
        String path_to_project = "/home/erik/Desktop/commons-math-test";
        //example: /home/erik/desktop/commons-math
        //Cheat iteration: in case you divide up the runs, add the cheat_iteration as the last Test suite ID.
        //end values that you can change
        
        //Initialize were all the files are located
        File dir_project = new File(path_to_project);
        File dir_realFiles = new File(path_to_project+ "/target/classes");
        File dir_mutantFiles = new File(path_to_project+"/target/pit-reports/export");
        File dir_erikFiles = new File(path_to_project+"/erik-files");
        File dir_tmp_dir = new File(path_to_project+"/erik-files/tmp_dir");
        
        //Create the tmp_dir nad erik-files if it doesn't exist yet
        if(!dir_erikFiles.exists()){
            dir_erikFiles.mkdir();
        }
        if(!dir_tmp_dir.exists()){
            dir_tmp_dir.mkdir();
        }
        //The path to were all files in the project are located
        Collection<File> collAllReal = findAllFiles(dir_realFiles);
        List<File> allReal = new ArrayList(collAllReal);
        //The path to were all mutants are located
        Collection<File> collAllMutants = findAllFiles(dir_mutantFiles);
        List<File> allMutants = readMutants(path_to_project);
        //Incase we run out of mutants (never happened for me).
        if (allMutants.size()<15){ 
            allMutants = new ArrayList(collAllMutants);
        }
        
        for(int x = 1;x<=iterations;x++){
            //This is the Test suite ID (Current iteration).
            System.out.println("CURRENT ITERATION: " + x);
            
            //Init variables
            Random rand = new Random();
            boolean last = false;
            int amountOfFiles = 0;
            
            //In order to make sure every file is changed. We can't pick the same file twice.
            //If the amount of files left (in the project) are less than 15 (aka most mutants) we reset.
            if(allReal.size()<=15){
                collAllReal = findAllFiles(dir_realFiles);
                allReal = new ArrayList(collAllReal);
                last = false;
            } else {
                //Get the amount of mutants selected for the current Test suite ID
                amountOfFiles = rand.nextInt(mostAmountOfFiles-leastAmountOfFiles)+leastAmountOfFiles;
            }
            
            //Actually run the program (Insert mutants etc into the compiled project and run starts)
            Map<String, List<File>> map = runProgram(amountOfFiles,allReal,allMutants,
                    dir_mutantFiles,dir_tmp_dir, dir_project, last,path_to_project, timeoutInSeconds);
            allReal = map.get("allReal");
            allMutants = map.get("allMutants");
            //Extract the data to file
            RetrieveData.extractData(x+cheat_iteration,path_to_project);
            try{
                //Save all the mutants that have not been selected.
                System.out.println("Saving mutants!");
                saveMutants(allMutants,dir_project);
            }catch (IOException e){
                System.out.println("Something went wrong when saving mutants!");
                e.printStackTrace();
            }
        }

    }
    public static Map<String, List<File>> runProgram(int amountOfFiles,List<File> allReal,List<File> allMutants,
                                        File dir_mutantFiles, File dir_tmp_dir, File dir_to_project
                                        , boolean last, String path_to_project, int timeoutInSeconds){
        //Init variables
        int failsafe = 0;
        int i = 0;
        HashMap<File,File> hm = new HashMap();
        List<File> pathsToReal = new ArrayList<>();
        while(i<amountOfFiles && allReal.size()>0){
            
            //Getting a single file
            File one_file = getRandomFile(allReal);
            String nameOfFile = getNameOfFile(one_file);
            //Find paths to the mutants with for one specfic file and select one.
            List<String> pathToMutants = new ArrayList(findSpecificFilesWithRemoval(allMutants,nameOfFile));
            if(pathToMutants.size()>0) {
                File one_mutation_file = getRandomMutantFile(pathToMutants);
                allMutants.remove(one_mutation_file);
                //If paths to mutants exists change the file and save the original compiled file
                //We do this so we don't have to recompile the program the "mvn compile" (takes time)
                if(pathToMutants!=null){
                    moveFileToDir(dir_tmp_dir,one_file);
                    File tmpReal = new File(dir_tmp_dir.getAbsolutePath() + "/" + one_file.getName());
                    hm.put(one_file,tmpReal);
                    copyFileToDir(one_file,one_mutation_file);
                    pathsToReal.add(one_file);
                }
                i++;
                allReal.remove(one_file);
            } else {
                allReal.remove(one_file);
            }
            failsafe++;
            if(failsafe==100){
                System.out.println("Triggered failsafe (in runProgram)!");
                break;
            } else if (failsafe==(amountOfFiles-1) && last == true){
                i = amountOfFiles;
            }
        }
        try {
            //Running starts
            System.out.println("Initializing maven starts:starts");
            //-Dmaven.main.skip avoids compiling the program so we still have our mutants when running starts.
            ProcessBuilder pb = new ProcessBuilder("mvn", "starts:starts", "-Dmaven.main.skip");
            pb.directory(new File(path_to_project));
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.directory(dir_to_project);
            Process process = pb.start();
            process.waitFor(timeoutInSeconds, TimeUnit.SECONDS);
            process.destroy();
            System.out.println("Running and waiting for process to finish!");
            System.out.println("HOPEFULLY WAITING HERE");
            System.out.println("Process finished running!!!");
        }catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
        replaceFiles(hm,pathsToReal);

        Map<String, List<File>> map = new HashMap<>();
        map.put("allReal",allReal);
        map.put("allMutants", allMutants);
        return map;
    }
    //Find all COMPILED (.class) files in directory
    public static Collection<File> findAllFiles(File rootDir) {
        final String[] SUFFIX = {"class"};  // use the suffix to filter
        Collection<File> files = FileUtils.listFiles(rootDir, SUFFIX, true);
        for (Iterator iterator = files.iterator(); iterator.hasNext(); ) {
            File file = (File) iterator.next();
            //System.out.println(file.getAbsolutePath());
        }
        return files;
    }
    //find specific mutant
    public static Collection<String> findSpecificFilesWithRemoval(List<File> allMutants, String fileName){
        String path;
        Collection<String> allPaths = new ArrayList<>();
        for (Iterator iterator = allMutants.iterator(); iterator.hasNext(); ) {
            File file = (File) iterator.next();
            if(!file.getName().equals(null) && file.getName().contains(fileName)){
                path = file.getAbsolutePath();
                allPaths.add(path);
            }
        }
        return allPaths;
    }
    //Find a specific file (.class) in directory
    public static Collection<String> findSpecificFiles(File rootDir, String fileName) {
        final String[] SUFFIX = {"class"};  // use the suffix to filter
        Collection<File> files = FileUtils.listFiles(rootDir, SUFFIX, true);
        String path = null;
        Collection<String> allPaths = new ArrayList<String>();
        for (Iterator iterator = files.iterator(); iterator.hasNext(); ) {
            File file = (File) iterator.next();
            if(!file.getName().equals(null) && file.getName().contains(fileName)){
                path = file.getAbsolutePath();
                allPaths.add(path);

            }
        }

        return allPaths;
    }
    //Returns name of one_file
    public static String getNameOfFile(File one_file){
        if(one_file.getName().contains("$")){
            //TODO:
            //make sure we cant modify one file twice?
        }
        return one_file.getName();
    }
    
    //Getting one random mutant file form a list
    public static File getRandomMutantFile(List<String> stringMutants){
        Random rand = new Random();
        String single_path = stringMutants.get(rand.nextInt(stringMutants.size()));
        File mutant_file = new File(single_path);
        return mutant_file;
    }
    //getting one random file
    public static File getRandomFile(List<File> allFiles){
        Random rand = new Random();
        return allFiles.get(rand.nextInt(allFiles.size()));
    }
    //Copy file to directory and rename it
    public static void moveFileToDir(File destination_dir,File file_to_copy){
        file_to_copy.renameTo(new File(destination_dir.getAbsolutePath() + "/" + file_to_copy.getName()));
    }
    //Copy the original file 
    public static void copyFileToDir(File destination_dir, File file_to_copy){
        try{
            FileUtils.copyFile(file_to_copy,destination_dir);
        }catch(IOException e){

        }
    }
    //Change back all real files
    public static void replaceFiles(HashMap<File,File> dirToTmpFiles,List<File> dirToRealFiles){
        for(File realFile : dirToRealFiles){
            File tmpReal = dirToTmpFiles.get(realFile);
            realFile.delete();
            tmpReal.renameTo(realFile);
        }
    }
    //Save notVisitedMutants
    public static void saveMutants (List<File> mutants, File dir_project) throws IOException{
        File erik_dir = new File(dir_project.getAbsolutePath()+"/erik-files");
        if(!erik_dir.exists()){
            boolean result = false;
            try{
                erik_dir.mkdir();
                result = true;
            }catch (SecurityException e){

            }
        }
        File erik_file = new File(erik_dir.getAbsolutePath()+"/mutantsNotVisited.txt");
        erik_file.createNewFile();

        BufferedWriter writer = new BufferedWriter(new FileWriter(erik_file));
        for(File file : mutants){
            String path = file.getAbsolutePath();
            writer.write(path + "\n");
        }
        writer.close();
    }
    //read mutants not visisted if we run the program multiple times.
    public static List<File> readMutants(String dir_project){
        List<File> notVisitedMutants = new ArrayList<>();
        File destination_dir = new File(dir_project);
        try {
            String line;
            File mutantsFile = new File(destination_dir.getAbsolutePath() + "/erik-files/mutantsNotVisited.txt");
            mutantsFile.createNewFile();
            FileReader fileReader = new FileReader(mutantsFile.getAbsolutePath());
            if(mutantsFile.exists()){
                BufferedReader br = new BufferedReader(fileReader);
                while ((line = br.readLine()) != null){
                    System.out.println(line);
                    File mutant = new File(line);
                    System.out.println(mutant);
                    notVisitedMutants.add(mutant);
                }
            }

        }catch (IOException e){
            System.out.println("DOES IT FAIL HERE?");
            e.printStackTrace();
        }
        return  notVisitedMutants;
    }
}

/*
* mvn -Dfeatures=+EXPORT org.pitest:pitest-maven:mutationCoverage
* Also added skip all tests in POM
* */
