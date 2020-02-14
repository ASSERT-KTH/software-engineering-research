import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class AddChangeHistory {
    String project_dir;

    public AddChangeHistory(String path_to_project) {
        project_dir = path_to_project;
    }
    //read the data output from STARTS
    public static List<List<String>> readAllData(String allData_dir) {
        List<List<String>> listAllData = new ArrayList<>();
        try {
            String line;
            File dir = new File(allData_dir + "/erik-files/allData.txt");
            FileReader fileReader = new FileReader(dir.getAbsolutePath());
            if (dir.exists()) {
                BufferedReader br = new BufferedReader(fileReader);
                while ((line = br.readLine()) != null) {
                    List<String> tmp = new ArrayList<>();
                    listAllData.add(Arrays.asList(line.split(",")));
                }
            }

        } catch (IOException e) {
            System.out.println("DOES IT FAIL HERE?");
            e.printStackTrace();
        }
        return listAllData;
    }
    //read the change history from the shellscript
    public static List<String> readChangeHistory(String proj_dir) {
        List<String> listChangeHistory = new ArrayList<>();

        try {
            String line;
            File destination_dir = new File(proj_dir + "/src/main/java/log.log");
            FileReader fileReader = new FileReader(destination_dir.getAbsolutePath());
            if (destination_dir.exists()) {
                BufferedReader br = new BufferedReader(fileReader);
                while ((line = br.readLine()) != null) {
                    listChangeHistory.add(line);
                }
            }

        } catch (IOException e) {
            System.out.println("DOES IT FAIL HERE?");
            e.printStackTrace();
        }
        return listChangeHistory;
    }
    //write the finalData
    public static void writeToFile(String dir_to_project, List<String> stuffToWrite) throws IOException {
        File file = new File(dir_to_project + "/erik_files/finalData.txt");
        String writeString = String.join(",", stuffToWrite) + "\n";
        if (!file.exists()) {
            file.createNewFile();
        }
        try {
            Files.write(Paths.get(file.getAbsolutePath()), writeString.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    //Run the program
    public static void runFinalData() throws IOException{
        //CHANGE THE PATH TO PROJECT HERE: -------------------
        String dir_project = "/home/erik/Desktop/commons-math";
        //END CHANGE PATH TO PROJECT -------------------------
        List<String> list = readChangeHistory(dir_project);
        Set<String> distinct = new HashSet<>(list);
        Map<String, Integer> frequencies = new HashMap<>();
        for (String s: distinct){
            int num = Collections.frequency(list, s);
            String[] javaFile = s.split("/");
            String fileName = javaFile[javaFile.length-1].replace(".java", "");
            //System.out.println(fileName);
            frequencies.put(fileName.toLowerCase(),num);
            if((fileName.toLowerCase()).equals("logit")){
                System.out.println(fileName + ": " + num);
            }
        }

        List<List<String>> allData = readAllData(dir_project);
        List<List<String>> finalData = new ArrayList<>();
        for (List<String> dataPoint : allData){
            List<String> copyData = new ArrayList<>();
            copyData.addAll(dataPoint);
            int connected_tests = Integer.parseInt(dataPoint.get(7));
            for(int i = 0; i<connected_tests;i++){
                String changed_file = dataPoint.get(9+i);
                String[] changedArray = changed_file.split("\\.");
                String changedName = changedArray[changedArray.length-1];
                int index = changedName.indexOf("$");
                if(index>0){
                    changedName = changedName.substring(0,index);
                }
                Integer freq = frequencies.get(changedName.toLowerCase());

                if(freq == null){
                    if(changedName.equals("BicubicFunction")){
                        changedName = "BicubicInterpolatingFunction";
                    } else if(changedName.equals("TricubicFunction")){
                        changedName = "TricubicInterpolatingFunction";
                    }
                    freq = frequencies.get(changedName.toLowerCase());
                    if(freq==null){
                        freq = 0;
                    }
                }
                String stringFreq = Integer.toString(freq);
                copyData.add(copyData.size()-1, stringFreq);
                System.out.println(copyData.toString());
            }
            writeToFile(dir_project,copyData);
            finalData.add(copyData);
        }
    }

    public static void main(String[] args) throws IOException {
        runFinalData();

    }
}
