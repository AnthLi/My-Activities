# -*- coding: utf-8 -*-
'''
Created on Wed Sep 21 16:02:58 2016

@author: cs390mb

Assignment 2 : Activity Recognition

'''

import os
import sys
import numpy as np
import matplotlib.pyplot as plt
from sklearn.tree import DecisionTreeClassifier, export_graphviz
from sklearn import svm
from features import extract_features
from util import slidingWindow, reorient, reset_vars
from sklearn import cross_validation
from sklearn.metrics import confusion_matrix
import pickle

# Load Data From Disk
print("Loading data...")
sys.stdout.flush()
data_file = os.path.join("data", "final-project-data.csv")
data = np.genfromtxt(data_file, delimiter= ",")
print("Loaded {} raw labelled activity data samples.".format(len(data)))
sys.stdout.flush()

# Pre-processing
print("Reorienting accelerometer data...")
sys.stdout.flush()
reset_vars()
reoriented = np.asarray([reorient(data[i, 1], data[i, 2], data[i, 3]) for i in range(len(data))])
reoriented_data_with_timestamps = np.append(data[:, 0:1],reoriented,axis = 1)
data = np.append(reoriented_data_with_timestamps, data[:, -1:], axis = 1)

# Extract Features & Labels
# you may want to play around with the window and step sizes
window_size = 20
step_size = 20

# sampling rate for the sample data should be about 25 Hz; take a brief window to confirm this
n_samples = 1000
time_elapsed_seconds = (data[n_samples, 0] - data[0, 0]) / 1000
sampling_rate = n_samples / time_elapsed_seconds

feature_names = [
  # 0, 1, 2
  "mean X", "mean Y", "mean Z",
  # 3, 4, 5
  "med X", "med Y", "med Z",
  # 6, 7, 8
  "std X", "std Y", "std Z",
  # 9
  "mean Mag",
  # 10
  "med Mag",
  # 11
  "std Mag",
  # 12, 13, 14
  "zCross X", "zCross Y", "zCross Z",
  # 15, 16, 17
  "min X", "min Y", "min Z",
  # 18, 19, 20
  "max X", "max Y", "max Z",
  # 21, 22, 23, 24, 25, 26, 27, 28, 29
  "ff1", "ff2", "ff3", "ff4", "ff5", "ff6", "ff7", "ff8", "ff9",
  # 30
  "entropy"
]

class_names = ["Sedentary", "Active"]

print("Extracting features and labels for window size {} and step size {}...".format(window_size, step_size))
sys.stdout.flush()

n_features = len(feature_names)

X = np.zeros((0, n_features))
y = np.zeros(0,)

for i, window_with_timestamp_and_label in slidingWindow(data, window_size, step_size):
  # omit timestamp and label from accelerometer window for feature extraction:
  window = window_with_timestamp_and_label[:, 1:-1]
  # extract features over window:
  x = extract_features(window)
  # append features:
  X = np.append(X, np.reshape(x, (1, -1)), axis = 0)
  # append label:
  y = np.append(y, window_with_timestamp_and_label[10, -1])

print("Finished feature extraction over {} windows".format(len(X)))
print("Unique labels found: {}".format(set(y)))
sys.stdout.flush()

# Plot data points
print("Plotting data points...")
sys.stdout.flush()
plt.figure()
formats = ["bo", "go", "ro", "mo"]
for i in range(0, len(y), 1): 
  plt.plot(X[i, 19], X[i, 11], formats[int(y[i])])
  # plt.plot(X[i, 7], X[i, 8], formats[int(y[i])])
  # plt.plot(X[i, 8], X[i, 11], formats[int(y[i])])

plt.show()

# Train & Evaluate Classifier
n = len(y)
n_classes = len(class_names)
cv = cross_validation.KFold(n, n_folds = 10, shuffle = True, random_state = None)

def _train_and_evaluate_classifier(classifier):
  acc = 0
  avgPrec = [0,0]
  avgRecall = [0,0]

  for i, (train_indexes, test_indexes) in enumerate(cv):
    X_train = X[train_indexes, :]
    y_train = y[train_indexes]
    X_test = X[test_indexes, :]
    y_test = y[test_indexes]
    classifier.fit(X_train, y_train)

    y_pred = classifier.predict(X_test)
    conf = confusion_matrix(y_test, y_pred, labels = [0, 1,2,3])
    print conf 
    # Add up the accuracies, precisions, and recalls
    diag = np.diag(conf)
    localAcc = 0
    for d in range(0, len(diag)):
      if conf.sum() != 0:
        localAcc += diag[d]/(conf.sum()*1.00000)
      if conf[:,d].sum() !=0:
        avgPrec[d] +=  diag[d]/(1.0000* conf[:,d].sum())
      if conf[d,:].sum() !=0:
        avgRecall[d] += diag[d]/(1.0000*conf[d,:].sum())
    acc += localAcc/(1.0000*len(diag))  

    print("Fold {}".format(i))

  # Print the calculated averages
  print "Average accuracy:", acc / 10
  print "Average precision:", avgPrec[0] / 10, avgPrec[1] / 10
  print "Average recall:", avgRecall[0] / 10, avgRecall[1] / 10

tree = DecisionTreeClassifier(criterion = "entropy", max_depth = 5, max_features = 30)

_train_and_evaluate_classifier(tree)

best_classifier = tree
with open("data/classifier.pickle", "wb") as f:
  pickle.dump(best_classifier, f)