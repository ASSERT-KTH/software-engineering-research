import pandas as pd
import xgboost as xgb
from xgboost import plot_importance
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, auc
from sklearn.metrics import precision_score
from sklearn.metrics import recall_score
from sklearn.metrics import confusion_matrix
from sklearn.metrics import roc_curve
from sklearn.metrics import log_loss
from sklearn.ensemble import RandomForestClassifier
import matplotlib.pyplot as plt
import numpy as np
import itertools
from bayes_opt import BayesianOptimization
from mlxtend.feature_selection import ExhaustiveFeatureSelector as EFS
from matplotlib import cm as conf_mat
import random
def plot_confusion_matrix(cm, classes,normalize=False,title='Confusion matrix',cmap=plt.cm.Blues):
    """
    This function prints and plots the confusion matrix.
    Normalization can be applied by setting `normalize=True`.
    """
    if normalize:
        cm = cm.astype('float') / cm.sum(axis=1)[:, np.newaxis]
        print("Normalized confusion matrix")
    else:
        print('Confusion matrix, without normalization')
    plt.figure()
    plt.imshow(cm, interpolation='nearest', cmap=cmap)
    plt.title(title)
    plt.colorbar()
    tick_marks = np.arange(len(classes))
    plt.xticks(tick_marks, classes, rotation=45)
    plt.yticks(tick_marks, classes)

    fmt = '.2f' if normalize else 'd'
    thresh = cm.max() / 2.
    for i, j in itertools.product(range(cm.shape[0]), range(cm.shape[1])):
        plt.text(j, i, format(cm[i, j], fmt),
                 horizontalalignment="center",
                 color="white" if cm[i, j] > thresh else "black")

    plt.ylabel('True label')
    plt.xlabel('Predicted label')
    plt.tight_layout()
    plt.gcf().savefig('data/cm.png')
    return cm

def rf_evaluate(n_estimators,max_depth,min_samples_split,max_features):
    rf = RandomForestClassifier(n_estimators=int(n_estimators),
                                min_samples_split=int(min_samples_split),
                                max_features=min(max_features, 0.999),
                                random_state=2,
                                max_depth= int(max_depth),
                                n_jobs=-1)
    X = X_dtrainRF.drop(["id","failed test"], axis=1)
    Y = X_dtestRF.drop(["id", "failed test"], axis=1)
    Xtarget = X_dtrainRF["failed test"]
    Ytarget = X_dtestRF["failed test"]
    rf.fit(X, Xtarget)
    allScores = rf.predict_proba(Y)[:,1]
    return -log_loss(Ytarget, allScores)

def xgb_evaluate(max_depth, gamma,learning_rate,n_estimators, colsample_bytree):
    params = {'eval_metric': 'rmse',
              'max_depth': int(max_depth),
              'subsample': 0.8,
              'learning_rate': float(learning_rate),
              'silent': 1,
              'gamma': gamma,
              'n_estimators': int(n_estimators),
              'colsample_bytree': colsample_bytree}
    # Used around 1000 boosting rounds in the full model
    xgbc = xgb.XGBClassifier(**params)
    Xtrain = X_dtrainXGB.drop(["id","failed test"], axis=1)
    Xtest = X_dtestXGB.drop(["id", "failed test"], axis=1)
    Ytrain = X_dtrainXGB["failed test"]
    Ytest = X_dtestXGB["failed test"]
    xgbc.fit(Xtrain,Ytrain)
    scorePred = xgbc.predict_proba(Xtest)[:,1]
    """predictions = []
    for value in scorePred:
        if value>=0.5:
            pred_value = 1
        else:
            pred_value = 0
        predictions.append(pred_value)"""
    return -log_loss(Ytest,scorePred)
    # Bayesian optimization only knows how to maximize, not minimize, so return the negative RMSE

def findParamsXGB(trainDF, testDF):
    global X_dtrainXGB
    global X_dtestXGB
    X_dtestXGB = testDF
    X_dtrainXGB = trainDF
    xgb_bo = BayesianOptimization(xgb_evaluate, {'max_depth': (10, 50),
                                                 'gamma': (0, 1),
                                                 'learning_rate': (0.001,0.3),
                                                 'n_estimators': (10,250),
                                                 'colsample_bytree': (0.3, 1.0)})
    xgb_bo.maximize(init_points=5 ,n_iter=15, acq='ei')
    print(xgb_bo.max)

def findParamsRF(trainDF, testDF):
    global X_dtrainRF
    global X_dtestRF
    X_dtestRF = testDF
    X_dtrainRF = trainDF
    xgb_bo = BayesianOptimization(rf_evaluate, {'n_estimators': (10,200),
                                                'max_depth': (3,50),
                                                'min_samples_split': (2,100),
                                                'max_features': (0.1,0.999)})
    xgb_bo.maximize(init_points=100,n_iter=100, acq="ei")
    print(xgb_bo.max)

def getFailureDetected(allDataDF):
    failureDetected = dict()
    for index,row in allDataDF.iterrows():
        id = int(row["id"])
        if id not in failureDetected:
            failureDetected[id] = 0
        if int(row["failed test"]) == 1:
            failureDetected[id] = 1
    return failureDetected

def plottingHighScore(y_predDF,allDataDF,y_pred_two, cm):
    #y_pred = y_predDF["pred"].tolist()
    y_pred = y_pred_two
    y_pred_id = y_predDF.drop("pred", axis=1)
    allDataDF_tmp = allDataDF.groupby(["id"])["failed test"].sum().clip(upper=1).reset_index(name = "one fail found")
    y_test = y_predDF["y_test"].tolist()
    fpr, tpr, threshold = roc_curve(y_test, y_pred)
    print("threshold",threshold)
    selectionRate = []
    changeRecall = []
    for limit in threshold:
        tmp = []
        y_pred_tmp = y_pred_id
        for pred in y_pred:
            if pred>limit:
                pred_value=0
            else:
                pred_value=1
            tmp.append(pred_value)
        y_pred_tmp["pred"] = tmp
        y_pred_tmp = y_pred_tmp.groupby(["id"]).sum().clip(upper=1)
        full_table = pd.merge(allDataDF_tmp,y_pred_tmp.drop("y_test",axis=1), on="id")
        pred_changeRecall = full_table["pred"].tolist()
        change_recall = sum(pred_changeRecall)/len(pred_changeRecall)
        changeRecall.append(change_recall)
        a = np.asarray(tmp)
        b = 1 - a
        y_opposite_test = 1 - np.asarray(y_test)
        recallThreshold = recall_score(y_opposite_test, b)
        recallThreshold = 1 - recallThreshold
        selectionRate.append(recallThreshold)
    npRecall = np.asarray(selectionRate)
    npChangeRecall = np.asarray(changeRecall)
    roc_auc = auc(npRecall, npChangeRecall)
    plt.figure()
    plt.title('Receiver Operating Characteristic')
    plt.plot(npRecall, npChangeRecall, 'b', label='AUC = %0.2f' % roc_auc)
    plt.legend(loc='lower right')
    plt.ylabel('change recall')
    plt.xlabel('selection rate')
    plt.gcf().savefig('data/roc_changerecall.png')
    c = np.savetxt("data/changeRecall.txt", changeRecall, delimiter=", ")
    c = np.savetxt("data/threshold.txt", threshold, delimiter=", ")
    c = np.savetxt("data/selectionRate.txt", selectionRate, delimiter=", ")
    class_names = ["0", "1"]
    plot_confusion_matrix(cm, classes=class_names, title="confusion_matrix")

def plottingStuff(y_predDF, cm):
    y_pred = y_predDF["pred"].copy()
    y_test = y_predDF["y_test"].copy()
    print(y_pred.tolist())
    print(y_test.tolist())
    print(y_pred)
    threshold = np.arange(-0.01,1.0002,0.0001)
    selectionRate = []
    recallRate = []
    for limit in threshold:
        print("LLIMMIT", limit)
        tmp = []
        for pred in y_pred:
            if pred>limit:
                pred_value=1
            else:
                pred_value=0
            tmp.append(pred_value)
        recall =  recall_score(y_test,np.array(tmp))
        print("recall: ", recall)
        recallRate.append(recall)
        a = np.array(tmp)
        b = 1 - a
        y_opposite_test = 1 - y_test
        recallThreshold = recall_score(y_opposite_test, b)
        print("selection rate: ", recallThreshold)
        recallThreshold = 1 - recallThreshold
        selectionRate.append(recallThreshold)
    roc_auc = auc(selectionRate,recallRate)
    plt.figure()
    plt.title('Receiver Operating Characteristic')
    plt.plot(selectionRate, recallRate, 'b', label='AUC = %0.2f' % roc_auc)
    plt.legend(loc='lower right')
    plt.xticks([0,0.5,1],[0,0.5,1])
    plt.ylabel('True Positive Rate')
    plt.xlabel('Selection Rate')
    plt.gcf().savefig('data/roc.png')
    plt.title('Receiver Operating Characteristic')
    c = np.savetxt("data/threshold.txt", threshold, delimiter=", ")
    c = np.savetxt("data/recall.txt", recallRate, delimiter=", ")
    c = np.savetxt("data/selectionRate.txt", selectionRate, delimiter=", ")
    class_names = ["0", "1"]
    plot_confusion_matrix(cm, classes=class_names, title="confusion_matrix")

def calculateChangerecall(y_pred_tmp,allDataDF_tmp):

    y_pred_tmp = y_pred_tmp.groupby(["id"]).sum().clip(upper=1)
    full_table = pd.merge(allDataDF_tmp, y_pred_tmp.drop("y_test", axis=1), on="id")
    pred_changeRecall = full_table["pred"].tolist()
    change_recall = sum(pred_changeRecall) / len(pred_changeRecall)
    return change_recall

def calculateSelectionRate(predictions,y_test):
    a = np.asarray(predictions)
    b = 1 - a
    y_opposite_test = 1 - np.asarray(y_test)
    recallOpposite = recall_score(y_opposite_test, b)
    SelectionRate = 1 - recallOpposite
    return  SelectionRate

def wrapper(x_train_df):
    x_train = x_train_df.drop(["id","failed test"], axis=1)
    y_train = x_train_df["failed test"]
    feature_selector = EFS(RandomForestClassifier(max_depth=17, n_estimators=136, max_features=0.307, min_samples_split=30
                                          ,random_state=42),
                           min_features=6,
                           max_features=7,
                           scoring='log_loss',
                           print_progress=True,
                           n_jobs=1,
                           cv=5)
    features = feature_selector.fit(x_train, y_train)
    print('Best recall score: %.2f' % feature_selector.best_score_)
    print('Best subset (indices):', feature_selector.best_idx_)
    print('Best subset (corresponding names):', feature_selector.best_feature_names_)
    print('Subsets_: ', feature_selector.subsets_)

def trainingRandomForest(x_train_df, x_test_df, allDataDF, plot=False):
    random_forest = RandomForestClassifier(max_depth=16, n_estimators=159, max_features=0.10577645690725401
                                           , min_samples_split=9,random_state=32)
    # Old: 17, 136, 0.307,30
    # Init data
    y_train = x_train_df["failed test"]
    y_test = x_test_df["failed test"]
    x_train = x_train_df.drop(["id","failed test"],axis=1)
    x_test = x_test_df.drop(["id","failed test"], axis=1)

    #train
    random_forest.fit(x_train, y_train)

    #predict
    y_pred = random_forest.predict_proba(x_test)
    y_pred_one = y_pred[:,1]
    print(y_pred_one.tolist())
    print("END HERE")

    #prepare data
    testID = x_test_df["id"].to_frame()
    testID["y_test"] = y_test
    testID["pred"] = y_pred_one
    testID_copy = testID.drop("pred", axis=1)
    class_names = ["0","1"]
    #Round values from predictions
    predictions = []
    allDataDF_tmp = allDataDF.groupby(["id"])["failed test"].sum().clip(upper=1).reset_index(name="one fail found")
    for value_one in y_pred_one:
        if value_one >8.499999999999890879e-03:
            pred_value = 1
        else:
            pred_value = 0
        predictions.append(pred_value)


    testID_copy["pred"] = predictions
    changeRecall = calculateChangerecall(testID_copy,allDataDF_tmp)
    accuracy = accuracy_score(y_test, predictions)
    precision = precision_score(y_test, predictions)
    recall = recall_score(y_test, predictions)
    cm = confusion_matrix(y_test, predictions)
    sr = calculateSelectionRate(predictions, y_test)
    # plotting roc-curve and confusion matrix
    if (plot):
        #plottingHighScore(testID, allDataDF,y_pred_two, cm)
        plottingStuff(testID, cm)
    plot_confusion_matrix(cm, classes=class_names, title="confusion_matrix")
    print("Selection Rate: %.2f%%" % (sr * 100.0))
    print("changeRecall: %.2f%%" % (changeRecall * 100.0))
    print("Accuracy: %.2f%%" % (accuracy * 100.0))
    print("Recall: %.2f%%" % (recall * 100.0))
    print("precision: %.2f%%" % (precision * 100.0))
    selectionRate = cm[0][1] / (cm[0][0] + cm[0][1])
    if(plot):
        plt.show()
    return selectionRate, recall

def trainingXgboost(train_df, test_df,allDataDF,plot=False,wrapper=False):
    x_train_noID = train_df.drop(["id","failed test"], axis=1)
    y_train = train_df["failed test"]
    y_test = test_df["failed test"]
    model = xgb.XGBClassifier(max_depth=35, n_estimators=506,
                             learning_rate=0.04804681857509842, gamma=0.3250774695085378,
                             colsample_bytree=.4733608893755554)
    model.fit(x_train_noID, y_train)
    if(wrapper):
        plot_importance(model,max_num_features=7)
        plt.show()
    y_predDF = pd.DataFrame(test_df["id"])
    y_pred = model.predict_proba(test_df.drop(["id","failed test"], axis=1))
    y_pred_one = y_pred[:,1]
    y_predDF["pred"] = y_pred[:,1]

    #prepare data
    testID = test_df["id"].to_frame()
    testID["y_test"] = y_test
    testID["pred"] = y_pred_one
    testID_copy = testID.drop("pred", axis=1)

    # Evaluation Accuracy,precision, recall and confusion matrix
    predictions = []
    for value in y_pred_one:
        if value >8.999999999999338390e-04:
            pred_value = 1
        else:
            pred_value = 0
        predictions.append(pred_value)
    testID_copy["pred"] = predictions
    allDataDF_tmp = allDataDF.groupby(["id"])["failed test"].sum().clip(upper=1).reset_index(name="one fail found")
    changeRecall = calculateChangerecall(testID_copy, allDataDF_tmp)
    sr = calculateSelectionRate(predictions, y_test)
    accuracy = accuracy_score(y_test, predictions)
    precision = precision_score(y_test, predictions)
    recall = recall_score(y_test, predictions)
    cm = confusion_matrix(y_test, predictions)
    #plotting roc-curve and confusion matrix
    if(plot):
        plottingStuff(testID,cm)
    print("change recall: %.2f%%" % (changeRecall * 100.0))
    print("Selection Rate: %.2f%%" % (sr * 100.0))
    print("Accuracy: %.2f%%" % (accuracy * 100.0))
    print("Recall: %.2f%%" % (recall * 100.0))
    print("precision: %.2f%%" % (precision * 100.0))
    selectionRate = cm[0][1]/(cm[0][0]+cm[0][1])
    if(plot):
        plt.show()
    return selectionRate,recall

def mainCorr():
    data = pd.read_csv("data/reducedData.txt", sep=",", header=None)
    data.columns = ["test runs", "File cardinality","Target cardinality","minimal distance"
                    , "connected tests","change history","failure rate", "failed test"]
    target = data["failed test"]
    #data = data.drop("failed test", axis=1)

    fig = plt.figure()
    ax1 = fig.add_subplot(111)
    cmap = conf_mat.get_cmap('jet', 200)
    cax = ax1.imshow(data.corr(), interpolation="nearest", cmap=cmap)
    ax1.grid(True)
    plt.title('Feature Correlation')
    labels = ["test runs", "File cardinality","Target cardinality","minimal distance"
        , "connected tests","change history","failure rate", "failed test"]
    ax1.set_xticks(np.arange(len(labels)))
    ax1.set_yticks(np.arange(len(labels)))
    ax1.set_xticklabels(labels,fontsize=6,rotation = 45)
    ax1.set_yticklabels(labels,fontsize=6,rotation = 45)
    fig.colorbar(cax, ticks=[-.2,-.1,0,.1,.2,.3,.4,.5,.6,.7,.8,.9,1])
    print(data.corr())
    plt.show()

def mainWrap():
    seed = 17
    data = pd.read_csv("data/reducedData.txt", sep=",", header=None)
    data.columns = ["id","test runs", "File cardinality","Target cardinality","minimal distance"
                    , "connected tests","change history","failure rate", "failed test"]
    target = data["failed test"]
    id = np.unique(data["id"])
    id_train = id
    train = data
    x_train_id, x_test_id, y_train_id, y_test_id = train_test_split(id_train, id, test_size=0.4, random_state=seed)
    #data = data.drop("id", axis=1)
    #data = data.drop("failed test", axis=1)

    #Create dataframes for merge
    x_train_df = pd.DataFrame(x_train_id)
    x_train_df.columns = ["id"]
    x_test_df = pd.DataFrame(x_test_id)
    x_test_df.columns = ["id"]

    #Merge dataframes
    x_train_df = pd.merge(data,x_train_df, on="id", sort=True)
    x_test_df = pd.merge(data,x_test_df, on="id", sort=True)
    wrapper(x_train_df)

def mainFeatureImp():
    seed = 17
    data = pd.read_csv("data/reducedData.txt", sep=",", header=None)
    data.columns = ["id","test runs", "File cardinality","Target cardinality","minimal distance"
                    , "connected tests","change history","failure rate", "failed test"]
    target = data["failed test"]
    data = data.drop("failed test", axis=1)
    train = data
    x_train, x_test, y_train, y_test = train_test_split(train, target, test_size=0.20, random_state=seed)
    sr, rec = trainingXgboost(x_train, x_test, y_train, y_test, plot=False, wrapper=True)

def mainFindParamsXGB():
    seed = 17
    data = pd.read_csv("data/reducedData.txt", sep=",", header=None)
    data.columns = ["id","test runs", "File cardinality","Target cardinality","minimal distance"
                    , "connected tests","change history","failure rate", "failed test"]
    id = np.unique(data["id"])
    id_train = id
    x_train_id, x_test_id, y_train_id, y_test_id = train_test_split(id_train, id, test_size=0.20, random_state=seed)

    #Create dataframes for merge
    x_train_df = pd.DataFrame(x_train_id)
    x_train_df.columns = ["id"]
    x_test_df = pd.DataFrame(x_test_id)
    x_test_df.columns = ["id"]

    #Merge dataframes
    x_train_df = pd.merge(data,x_train_df, on="id", sort=True)
    x_test_df = pd.merge(data,x_test_df, on="id", sort=True)

    findParamsXGB(x_train_df, x_test_df)

def mainFindParamsRF():
    seed = 17
    data = pd.read_csv("data/reducedData.txt", sep=",", header=None)
    data.columns = ["id","test runs", "File cardinality","Target cardinality","minimal distance"
                    , "connected tests","change history","failure rate", "failed test"]
    id = np.unique(data["id"])
    id_train = id
    x_train_id, x_test_id, y_train_id, y_test_id = train_test_split(id_train, id, test_size=0.35, random_state=seed)

    #Create dataframes for merge
    x_train_df = pd.DataFrame(x_train_id)
    x_train_df.columns = ["id"]
    x_test_df = pd.DataFrame(x_test_id)
    x_test_df.columns = ["id"]

    #Merge dataframes
    x_train_df = pd.merge(data,x_train_df, on="id", sort=True).drop("File cardinality", axis=1)
    x_test_df = pd.merge(data,x_test_df, on="id", sort=True).drop("File cardinality", axis=1)

    findParamsRF(x_train_df, x_test_df)

def mainRF():
    seed = 170
    data = pd.read_csv("data/reducedData.txt", sep=",", header=None)
    data.columns = ["id","test runs", "File cardinality","Target cardinality","minimal distance"
                    , "connected tests","change history","failure rate", "failed test"]
    id = np.unique(data["id"])
    id_train = id
    sr = []
    rec = []
    random.seed(seed)
    rand = random.randint(1,20)
    for i in range(10):
        randVar = i*rand
        x_train_id, x_test_id, y_train_id, y_test_id = train_test_split(id_train, id, test_size=0.2, random_state=randVar)

        #Create dataframes for merge
        x_train_df = pd.DataFrame(x_train_id)
        x_train_df.columns = ["id"]
        x_test_df = pd.DataFrame(x_test_id)
        x_test_df.columns = ["id"]

        #Merge dataframes
        x_train_df = pd.merge(data,x_train_df, on="id", sort=True).drop("File cardinality", axis=1)
        x_test_df = pd.merge(data,x_test_df, on="id", sort=True).drop("File cardinality", axis=1)

        #training

        selRate, recall = trainingRandomForest(x_train_df, x_test_df, data,plot=False)
        sr.append(selRate)
        rec.append(recall)
    print("Averagee selection rate: ", sum(sr)/len(sr))
    print("Average recall: ", sum(rec) / len(rec))
    print("Variance selection rate: ", np.var(np.array(sr)))
    print("Variance recall: ", np.var(np.array(rec)))

def mainXGB():
    #TODO: denna är inte klar
    seed = 17
    data = pd.read_csv("data/reducedData.txt", sep=",", header=None)
    data.columns = ["id","test runs", "File cardinality","Target cardinality","minimal distance"
                    , "connected tests","change history","failure rate", "failed test"]

    #traing data depending on ID
    id = np.unique(data["id"])
    id_train = id
    rec = []
    sr = []
    for i in range(1):
        x_train_id, x_test_id, y_train_id, y_test_id = train_test_split(id_train, id, test_size=0.2, random_state=i)

        #Create dataframes for merge
        x_train_df = pd.DataFrame(x_train_id)
        x_train_df.columns = ["id"]
        x_test_df = pd.DataFrame(x_test_id)
        x_test_df.columns = ["id"]

        #Merge dataframes
        x_train_df = pd.merge(data,x_train_df, on="id", sort=True)
        x_test_df = pd.merge(data,x_test_df, on="id", sort=True)

        #training
        selectionRate,recall = trainingXgboost(x_train_df, x_test_df,data, plot=True)
        sr.append(selectionRate)
        rec.append(recall)
    print("Averagee selection rate: ", sum(sr)/len(sr))
    print("Average recall: ", sum(rec) / len(rec))
    print("Variance selection rate: ", np.var(np.array(sr)))
    print("Variance recall: ", np.var(np.array(rec)))

if __name__ == '__main__':
    mainRF()