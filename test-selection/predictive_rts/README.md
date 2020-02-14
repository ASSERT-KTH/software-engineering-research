# Guide

This will be a guide of how to use eal predictive rts. 
Disclaimer: not meant to be used for productioń

Used in this project:
- Java 8
- Maven 3.6.0
- PiTest 1.4.7
- OS: Linux
- Project to run on: Commons-math (version 3.6.1) link: https://github.com/apache/commons-math
- (Python 3.6 for machine learning)

Table of content
1. Overview
2. Installation
3. Usage
4. Dataoutput (full data)
5. Machine learning and results

## 1. Overview
An overview of how the generation of data works. I will use these steps (below) in the usage section to explain how to perform them. 
1. Generate mutants with pitest 
2. Run starts on the project and store checksums
3. Insert compiled mutants into the project
4. Run starts without storing checksums on the project with mutants
5. Extract the output from starts
6. Restore to the original project
7. Repeat from step from step 3-7 for X iterations
8. (get change history of files)

![Picture of the workflow to generate data](https://github.com/kth-tcs/kth-test-selection/blob/master/eal%20predictive%20rts/pictures/Workprocess_new.png)

# 2. Installation
## Installation of PiTest
Add the following to the projects pom.xml:
```
        <plugin>
                <groupId>org.pitest</groupId>
                <artifactId>pitest-maven</artifactId>
                <version>1.4.7</version>
                <configuration>
                        <excludedTestClasses>
                                <param> *Test </param>
                        </excludedTestClasses>
                </configuration>
        </plugin>
      
```
That's it, you are up and running.

Additional information: 
Link to PiTest: http://pitest.org/quickstart/maven/

## Installation of modified STARTS
My starts version is modified. If curious, original starts project can be found here: https://github.com/TestingResearchIllinois/starts
- Unzip the starts-starts-1.3.tar.xz
- cd starts-starts-1.3/starts-plugin/src/main/java/edu/illinois/starts/jdeps
- Open RunMojo.java in texteditor
- In the exportData method
        - change line 130: String path_to_write = "/path/to/project";
        to the path to the project.
- go back to, cd starts-starts-1.3
- mvn install -Dcheckstyle.skip
- add the following to the projects pom.xml
```
        <plugin>
                <groupId>edu.illinois</groupId>
                <artifactId>starts-maven-plugin</artifactId>
                <version>1.3</version>
        </plugin>
```
## Installation of eal_generateData
- open ReplaceCompiledFiles.java in editor
        - in main, change line 18 (path_to_project="/path/to/project") to the path to the project.
        - in main, you can also change iterations (See picture), least- and most amount of files to change for each               iteration, timeoutinseconds.
        
**NOTE: A full test run for me takes up to 3 minutes. Perform all tests (mvn test) and set the timeout variable slightly above that. The timeout exist because mutants can occasionally cause infinite loops**

# 3. Usage
### 1. Generate mutants with pitest

- cd /path/to/project
- mvn -Dfeatures=+EXPORT org.pitest:pitest-maven:mutationCoverage

**NOTE: The last command run will cause mutants to persisting to disk and can take up alot of storage. So make sure that you have space before running this command.**

Additional information:
For this thesis we only want the mutants. Which is why the excludedTestClasses are added to our pom.xml, this will cause test not to be run.
```
                        <excludedTestClasses>
                                <param> *Test </param>
                        </excludedTestClasses>
```
The mutants will be saved in /path/to/project/target/pit-reports

### 2. Initial run with starts on the project and store updateRunChecksums
- cd /path/to/project
- mvn starts:starts -DupdateRunChecksums=True

Additional information:
The original version of starts updateRunChecksum=True by default. However, for this thesis we set it to False by default because we do not want to update the checksums after each test run. If checsksums would be stored after each run it will cause the starts to detect changes when swap back the original, already working, code.

### 3-7 run ReplaceCompiledFiles
This program will do the following:
3. Insert compiled mutants into the project
4. Run starts without storing checksums on the project with mutants
5. Extract the output from starts
6. Restore to the original project
7. Repeat from step from step 3. for X iterations

- mvn install
- mvn exec:java -Dexec.mainClass=ReplaceCompiledFiles

**NOTE: this can process can take quite some time.**

### Optional: (8. get change history for files)
- Run the change_file.sh script in /path/to/project/src/main/java to get the change history.
- On line 75 in AddChangeHistory.java add the current path.
- Run main in AddChangeHistory.java and get the data.

### Dataoutput
The output will be found in path/to/project/erik-files/allData.txt
alternative if run changeHistory path/to/project/erik-files/finalData.txt

(The output of this process can be found on this github in: data/finalData.txt)

## Machine learning
This entire approach uses sklearn. (a part from xgboost) In the "Machine_learning" directory you can also find a setup.py with specified install_requirements.

### Usage preprocessing.py
This will take the finalData.txt file and convert it to reducedData.txt which will be the input to our machine learning model.
The finalData.txt should be in the "data" subfolder of the preprocessing.py file. Run py file and it will output the reduced data.

### Usage training.py
Run the mainXGB() or mainRF() to train the network.

Available options:
it is possible to explore hyperparameters by calling (mainFindParamsRF() or mainFindParamsXGB())
It is possible to explore correlation matrix (mainCorr())

### Extracting features and Preparing data
The features are based on: https://arxiv.org/pdf/1810.05286.pdf (Machalica et al) and https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/45861.pdf (memon et al) and are found below
The data output from the data generation process can be found in "data/finalData.txt". From this we extract:
- 0 = test runs (extracted)
- 1 = test failures (extracted)
- 2 = test errors (extracted)
- 3 = test skipped (Not extracted)
- 4 = file cardinality (extracted)
- 5 = affected tests (extracted)
- 6 = minimal distance (extracted)
- 7 = num connected tests (extracted)
- 8 = what test failed (Not extracted)
- 9 = shortest distance file (Not extracted)
- 10+num connected test = connected tests (Not extracted)
- 10+num connected test + num  connected tests = change history (extracted)
- 10+num connected test + num  connected tests+1 = Test suite ID (extracted)
- 10+num connected test + num  connected tests+2 = Failure rates (extracted/calculated)

The train/test set is split based on test suite ID and the approximately have slightly under 800 different test suites. And above 80000 individual data points.

### Parameter tuning for XGBoost and Random Forest
I use a bayesian optimization approach to find hyperparamters: https://github.com/fmfn/BayesianOptimization
which tries to minimize a log_loss function

### Feature importance and correlation
- Feature had a general low correlation (Fairly close to zero)
- Used a wrapper method (ExhaustiveFeatureSelection) and the auc-roc curve. Removed "File cardinality" (Table coming soon with values from the selection as well as roc-curve.)
### Metrics
![equation](https://latex.codecogs.com/gif.latex?%5Ctext%7Brecall%7D%3D%5Cfrac%7BIndividual%5C%3A%20test%5C%3A%20failure%5C%3A%20identified%5C%3A%20by%20%5C%3Aeal%7D%7BIndividual%5C%3Atest%20%5C%3Afailure%5C%3Aidentified%20%5C%3Aby%20%5C%3ASTARTS%7D)

![equation](https://latex.codecogs.com/gif.latex?%5Ctext%7BSelection%5C%3A%20rate%7D%3D%5Cfrac%7BNon%20Test%5C%3A%20failure%5C%3A%20missclassified%5C%3A%20by%20%5C%3Aeal%7D%7BNon%20Test%5C%3Afailures%5C%3A%20identified%20%5C%3Aby%20%5C%3ASTARTS%7D)

![equation](https://latex.codecogs.com/gif.latex?%5Ctext%7BChange%5C%3A%20recall%7D%3D%5Cfrac%7BFaulty%5C%3A%20change%5C%3A%20identified%5C%3A%20in%5C%3A%20the%5C%3A%20test%5C%3A%20suite%5C%3A%20by%20%5C%3Aeal%7D%7BFaulty%20%5C%3Achange%20%5C%3Aidentified%5C%3A%20in%5C%3A%20the%5C%3A%20test%5C%3A%20suite%20%5C%3Aby%20%5C%3ASTARTS%7D)

# Results

### build information
This picture presents build information when inserting mutants into a project.

![Picture](https://github.com/kth-tcs/kth-test-selection/blob/master/eal%20predictive%20rts/pictures/InsertedMutantsBuildSuccessRateTwo.PNG)

### Results for machine learning
Machalica et al. (https://arxiv.org/pdf/1810.05286.pdf) had a recall of 95% and reduced the amount of tests by a factor of two (i.e. selection rate  = 50%) with random forest, the preliminary results shows that Random forest performs better than xgboost when the recall rate is lowered from 99% to 95%.

![Picture](https://github.com/kth-tcs/kth-test-selection/blob/master/eal%20predictive%20rts/pictures/table5_25%25split.PNG)

![Picture](https://github.com/kth-tcs/kth-test-selection/blob/master/eal%20predictive%20rts/pictures/table6_10%25split.PNG)

![Picture](https://github.com/kth-tcs/kth-test-selection/blob/master/eal%20predictive%20rts/pictures/table7_comparison.PNG)

### ROC-curve XGBoost
This shows the ROC-curve of XGBoost with area under the curve, auc.

![Picture](https://github.com/kth-tcs/kth-test-selection/blob/master/eal%20predictive%20rts/pictures/Roc-curve_XGB.PNG)

This compares the roc curves of XGBoost and a simple test selector, which chooses tests based on the failure rates.

![Picture](https://github.com/kth-tcs/kth-test-selection/blob/master/eal%20predictive%20rts/pictures/Roc-curve_XGBvsSIMPLE.PNG)
### ROC-curve Random Forest

This shows the Roc-curve of the RF with area under the curve, AUC.

![Picture](https://github.com/kth-tcs/kth-test-selection/blob/master/eal%20predictive%20rts/pictures/Roc-curve_RF.PNG)

This compares how well the Random Forest compares to a simple test selector, which chooses test based on the failure rates.

![Picture](https://github.com/kth-tcs/kth-test-selection/blob/master/eal%20predictive%20rts/pictures/Roc-curve_RFvsSIMPLE.PNG)
### XGB vs RF roc-curve
This picture shows how Random Forest performs in comparison to XGBoost. 

![Picture](https://github.com/kth-tcs/kth-test-selection/blob/master/eal%20predictive%20rts/pictures/XGBvsRF2.PNG)
## Findings
This approach managed to reduce the Average selection rate with about 57.1% while still managed to have a recall of 95%. For higher recall, the models performs similar. However, when lowering the recall value, Random Forest seem to select fewer test according to my results. However since their tool is not directly open source no comparison can be made. The data that Machalica et al used were histroical commits and test result, which means that they had a time aspects of the faulty changes and commits. This approach does not have such data.


