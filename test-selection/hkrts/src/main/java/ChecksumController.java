
import java.util.ArrayList;
import java.io.*;
import java.security.MessageDigest;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;


/**
* This class only handles the storage and controlling of changed checksums.
* Any work which depends on code changes (which is detected by changed checksums)
* should be done in another class dedicated to that.
*
*/
public class ChecksumController {

String pathToChecksumFile = System.getProperty("user.dir"); //Path not needed if file placed in same dir as program execute in?
String nameOfChecksumFile = "recordedChecksums.txt";

	/**
	* Constructor. 
	*/
	public ChecksumController(){

	}
	
	/**
	* Need one method to send the data to other classes
	*/
	public ArrayList<String> getChangedChecksums(String inputPathToClassFiles) { //Other classes use this, which calls the local methods that does all the work, and then this returns the result of that work.
		String pathToClassFiles = inputPathToClassFiles;
		File originFolder = new File(pathToClassFiles);
		ArrayList<File> filePrepList = new ArrayList<File>(); //Because I am tired, this is the solution at the moment.
		System.out.println("The path to the origin folder: " + originFolder.getPath());
		ArrayList<File> files = new ArrayList<File>(getAllClassFilesUnderDirectory(originFolder, filePrepList));
		System.out.println("Number of classfiles found under the starting Dir: " + files.size());
		Map<String, String> checkSums = new HashMap<String, String>();
		
		for (File classfile : files) {
			try {
				checkSums.put(classfile.getName(), getMD5Checksum(classfile));
			} catch (Exception e) {
				System.out.println(e.toString());
				e.printStackTrace();
			}
		}
		System.out.println("Number of elements in checksum Map: " + checkSums.size());
			
		ArrayList<String> classesToTest = new ArrayList<String>(controlChecksums(checkSums)); //TODO controlChecksums seems to be the villain ATM
		
		//@Return: Returns the names of classes with changed checksums so that the test selector class can select the correct tests
		return classesToTest;
	}
	
	/**
	* Need one method to get the data
	*/
	//Translates the byte array to HEX String
	private String getMD5Checksum(File filename) throws Exception {
		byte[] b = createChecksum(filename);
		String result = "";

		for (int i=0; i < b.length; i++) {
			result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
		}
		return result;
	}
	
	//returns a byte array which is the checksum of the file
	private byte[] createChecksum(File filename) {
		byte[] buffer = new byte[2048];
		try {
			InputStream fis =  new FileInputStream(filename);
					
			MessageDigest complete = MessageDigest.getInstance("MD5");
			int numRead = 0;
			
			while (numRead != -1) {
				numRead = fis.read(buffer);
				if (numRead > 0) {
					complete.update(buffer, 0, numRead);
				}
			}

			fis.close();
			return complete.digest();
		} catch (Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}
		return buffer;
	}
	
	/**
	* Need one method to check for changed Checksums;
	* All input values not in DB are added to DB. All values in DB not in input are removed from DB. Values in both DB and input are compared, if different the DB updates.
	* All values which are added to DB or updated in DB are added to the list of classes to be tested.
	*/
	private ArrayList<String> controlChecksums(Map<String, String> inputChecksums) {
		ArrayList<String> classesToBeTested = new ArrayList<String>();
		ArrayList<String> linesToWriteToFile = new ArrayList<String>();
		HashMap<String, String> checksums = new HashMap<String, String>(inputChecksums);

		String completeChecksumFilePath = pathToChecksumFile + "//" + nameOfChecksumFile;
		File checksumFile = new File(completeChecksumFilePath);
		if (!checksumFile.exists()) {
			try {
				checksumFile.createNewFile();
			} catch (Exception e) {
				System.out.println(e.toString());
				e.printStackTrace();
			}
		}
		ArrayList<String> linesOfChecksumFile = new ArrayList<String>();
		linesOfChecksumFile = readFromFile(checksumFile);
		
		ArrayList<String> classNamesOnFile = new ArrayList<String>();
		ArrayList<String> checksumsOnFile = new ArrayList<String>();
		for (String line : linesOfChecksumFile) {
			String[] parts = line.split(", checksum:");
			classNamesOnFile.add(parts[0]);
			checksumsOnFile.add(parts[1]);
		}
		
		if (checksums.entrySet().size()<linesOfChecksumFile.size()) {
			for (String classNameOnFile : classNamesOnFile) {
				if (checksums.containsKey(classNameOnFile)) {
					String checkSumOnFile = checksumsOnFile.get(classNamesOnFile.indexOf(classNameOnFile));
					if (checksums.get(classNameOnFile).equals(checkSumOnFile)) {
						String line = "" + classNameOnFile + ", checksum:" + checksumsOnFile.get(classNamesOnFile.indexOf(classNameOnFile)); //I'm sorry for this
						linesToWriteToFile.add(line); //Just add the line back to the file unchanged
						continue;
					}
					//If checksums are different add it to be tested and write the new checksum to checksum file
					classesToBeTested.add(classNameOnFile);
					String newLine = "" + classNameOnFile + ", checksum:" + checksums.get(classNameOnFile);
					linesToWriteToFile.add(newLine);
				} 
				//if a line reaches here it means the class is not in the input anymore and should be removed from checksum file, and thus is not added to linesToWriteToFile
			}
		} else { 
			for (Object key : checksums.keySet()) {
				String className = key.toString();
				String checkSum = checksums.get(key);
				boolean doesClassExistOnChecksumFile = false;
				
				for (String classNameOnFile : classNamesOnFile) {
					String line = "" + className + ", checksum:" + checkSum; //checkSum is always as new or newer than the checkSumOnFile, making this always correct(?)
					String checkSumOnFile = checksumsOnFile.get(classNamesOnFile.indexOf(classNameOnFile));
					if (className.equals(classNameOnFile) && (checkSum.equals(checkSumOnFile))) {
						doesClassExistOnChecksumFile = true;
						linesToWriteToFile.add(line);
					} else if (className.equals(classNameOnFile)) {
						doesClassExistOnChecksumFile = true;
						linesToWriteToFile.add(line);
						classesToBeTested.add(className);
					}
				}
				if (doesClassExistOnChecksumFile == false) {
					String line = "" + className + ", checksum:" + checkSum;
					linesToWriteToFile.add(line);
					classesToBeTested.add(className);
				}
			}
		}
		System.out.println("NUMBER OF CLASSES TO TEST: " + classesToBeTested.size());//DELET
		writeToFile(checksumFile, linesToWriteToFile);
		
		return classesToBeTested;
	}
	
	
	/**
	* Help methods
	*/
	
	/**
	* PrintWriter will overwrite the old file when it opens anew and starts printing.
	* If one wishes to append to the end of a file, maybe moving the pointer of the printwriter to EoF before printing will save the old text?
	*/
	private void writeToFile(File file, ArrayList<String> lines) {
		try {
			PrintWriter writer = new PrintWriter(file, "UTF-8");
			for (String line : lines) {
				writer.println(line);
			}
			writer.close();
		} catch (Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}
	}
	private ArrayList<String> readFromFile(File checksumFile) {
		ArrayList<String> lines = new ArrayList<String>();
		try {
			FileReader fReader = new FileReader(checksumFile);
			BufferedReader bReader = new BufferedReader(fReader);
			
			while (bReader.ready()) {
				String line = bReader.readLine();
				lines.add(line);
			}
			
			bReader.close();
			fReader.close();
		} catch(Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
		}
		return lines;
	}
	
	//If a file is a file, the it is added to the list of files if it is a class.
	//If a file is a directory, the function is recrusively called with the pointer inside of that directory
	private ArrayList<File> getAllClassFilesUnderDirectory(File folder, ArrayList<File> appendList) {
		for (File fileEntry : folder.listFiles()) { //split this and see if error persists, on what line
			if (fileEntry.isDirectory()) {
				getAllClassFilesUnderDirectory(fileEntry, appendList);
			} else if (fileEntry.getName().toLowerCase().endsWith(".class")){
				appendList.add(fileEntry);
			}
		}
		return appendList;
	}
}