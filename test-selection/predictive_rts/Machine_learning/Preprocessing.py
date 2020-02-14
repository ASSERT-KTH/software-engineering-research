class ImportData:
    def __init__(self, pathFile):
        self.path = pathFile
        self.finalData = []
        #Binary if the test run failed or not (not actually how many test failed).
        #Counts if a failure is raised or not
        self.failureBinaryCount = dict()
        self.successBinaryCount = dict()
        #Counts how many test failed.
        self.failureCount = dict()
        self.successCount = dict()
        # data with failure rates
        self.dataWithFailureRates = []
    def readData(self):
        with open(self.path) as infile:
            for line in infile:
                line = line.rstrip("\n")
                self.finalData.append([x.strip() for x in line.split(",")])

    #Extract failure rates
    def analyzeData(self):
        for item in self.finalData:
            test_array = item[8].split(".")
            test = test_array[len(test_array)-1]
            if "$" in test:
                test = test.split("$")[0]
            testRun = item[0]
            testFailure = item[1]
            testError = item[2]
            if (int(testFailure)>0) or (int(testError)>0):
                testBinaryFailure = 1
                testBinarySuccess = 0
            else:
                testBinaryFailure = 0
                testBinarySuccess = 1
            self.failureBinaryCount[test] = self.failureBinaryCount.get(test, 0) + testBinaryFailure
            self.failureCount[test] = self.failureCount.get(test, 0) + (int(testFailure)+int(testError))
            self.successBinaryCount[test] = self.successBinaryCount.get(test, 0) + testBinarySuccess
            self.successCount[test] = self.successCount.get(test, 0) + (int(testRun)-int(testError)-int(testFailure))
        print("failurebinary: " , self.failureBinaryCount)
        for key in sorted(self.failureBinaryCount.keys()):
            print("%s: %s" % (key, self.failureBinaryCount[key]))
    def getBinaryFailureRate(self, test):
        binaryFailureRate = int(self.failureBinaryCount[test])/(int(self.successBinaryCount[test])+int(self.failureBinaryCount[test]))
        return binaryFailureRate
    def getFailureRate(self,test):
        failureRate = int(self.failureCount[test])/(int(self.failureCount[test])+int(self.successCount[test]))
        return failureRate
    def getUnporcessedData(self):
        return self.finalData;
    def addFailureRate(self, write=False):
        if(write):
            f = open("data/reducedData.txt","w+")
        seperator = ","
        for item in self.finalData:
            test_array = item[8].split(".")
            test = test_array[len(test_array)-1]
            if "$" in test:
                test = test.split("$")[0]
            print(test, " has the failure rate of: ", self.getBinaryFailureRate(test))
            item.append(self.getBinaryFailureRate(test))
            conTest = int(item[7])
            total_change = 0
            for i in range(conTest):
                total_change += int(item[len(item)-i-3])
            if(int(item[1])>0 or int(item[2])>0):
                binaryFailed = 1
            else:
                binaryFailed = 0
            tmp = [item[len(item)-2],item[0],item[4],item[5],item[6], item[7],total_change,item[len(item)-1],binaryFailed]
            self.dataWithFailureRates.append(tmp)
            stringToWrite = seperator.join(map(str, tmp)) + "\n"
            if(write):
                f.write(stringToWrite)
        if(write):
            f.close()
    def printInfoFinalData(self):
        print("Data points: ", self.finalData[0]);


importObject = ImportData("data/finalData.txt")
importObject.readData()
importObject.analyzeData()
importObject.addFailureRate(True)

# 0 = test runs
# 1 = test failures
# 2 = test errors
# 3 = test skipped
# 4 = file cardinality
# 5 = affected tests
# 6 = minimal distance
# 7 = num connected tests
# 8 = what test failed
# 9 = shortest distance file
# 10+num connected test = connected tests
# 10+num connected test + num  connected tests = change history
# 10+num connected test + num  connected tests+1 = current iteration
# 10+num connected test + num  connected tests+2 = Failure rates
