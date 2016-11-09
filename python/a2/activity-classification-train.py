# -*- coding: utf-8 -*-
'''
Created on Wed Sep 21 16:02:58 2016

@author: cs390mb

Assignment 2 : Activity Recognition

This is the starter script used to train an activity recognition
classifier on accelerometer data.

See the assignment details for instructions. Basically you will train
a decision tree classifier and vary its parameters and evalute its
performance by computing the average accuracy, precision and recall
metrics over 10-fold cross-validation. You will then train another
classifier for comparison.

Once you get to part 4 of the assignment, where you will collect your
own data, change the filename to reference the file containing the
data you collected. Then retrain the classifier and choose the best
classifier to save to disk. This will be used in your final system.

Make sure to chek the assignment details, since the instructions here are
not complete.

'''

import os
import sys
import numpy as np
import matplotlib.pyplot as plt
from sklearn.tree import DecisionTreeClassifier, export_graphviz
from sklearn import svm
from features import extract_features # make sure features.py is in the same directory
from util import slidingWindow, reorient, reset_vars
from sklearn import cross_validation
from sklearn.metrics import confusion_matrix
import pickle


# %%---------------------------------------------------------------------------
#
#		                 Load Data From Disk
#
# -----------------------------------------------------------------------------

print("Loading data...")
sys.stdout.flush()
data_file = os.path.join("data", "my-activity-data.csv")
data = np.genfromtxt(data_file, delimiter= ",")
print("Loaded {} raw labelled activity data samples.".format(len(data)))
sys.stdout.flush()

# %%---------------------------------------------------------------------------
#
#		                    Pre-processing
#
# -----------------------------------------------------------------------------

print("Reorienting accelerometer data...")
sys.stdout.flush()
reset_vars()
reoriented = np.asarray([reorient(data[i, 1], data[i, 2], data[i, 3]) for i in range(len(data))])
reoriented_data_with_timestamps = np.append(data[:, 0:1],reoriented,axis = 1)
data = np.append(reoriented_data_with_timestamps, data[:, -1:], axis = 1)

# %%---------------------------------------------------------------------------
#
#		                Extract Features & Labels
#
# -----------------------------------------------------------------------------

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
class_names = ["Walking", "Stationary", "Jumping", "Biking"]

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

# %%---------------------------------------------------------------------------
#
#		                    Plot data points
#
# -----------------------------------------------------------------------------

# We provided you with an example of plotting two features.
# We plotted the mean X acceleration against the mean Y acceleration.
# It should be clear from the plot that these two features are alone very uninformative.
print("Plotting data points...")
sys.stdout.flush()
plt.figure()
formats = ["bo", "go", "ro", "mo"]
# for i in range(0, len(y), 10): # only plot 1/10th of the points, it"s a lot of data!
#   # plt.plot(X[i, 3], X[i, 8], formats[int(y[i])])
#   plt.plot(X[i, 7], X[i, 8], formats[int(y[i])])
#   # plt.plot(X[i, 8], X[i, 11], formats[int(y[i])])

# plt.show()

# %%---------------------------------------------------------------------------
#
#		                Train & Evaluate Classifier
#
# -----------------------------------------------------------------------------

n = len(y)
n_classes = len(class_names)

# TODO: Train and evaluate your decision tree classifier over 10-fold CV.
# Report average accuracy, precision and recall metrics.

cv = cross_validation.KFold(n, n_folds = 10, shuffle = True, random_state = None)

# Compute the accuracy, average precision, and average recall
def _compute_values(matrix):
  acc = 0
  avgPrecA = avgRecA = 0
  avgPrecB = avgRecB = 0
  avgPrecC = avgRecC = 0
  avgPrecD = avgRecD = 0

  npMatrix = np.matrix(matrix)

  # Calculate the accuracy
  acc = npMatrix.diagonal().sum() / float(npMatrix.sum())

  # Calculate the precision and recall of A
  tpA = npMatrix[0, 0]
  colSumA = npMatrix[:, 0].sum(0)
  rowSumA = npMatrix[0, :].sum(1)

  # Guard against division by 0
  if tpA != 0 and colSumA != 0:
    avgPrecA = tpA / float(colSumA)

  if tpA != 0 and rowSumA != 0:
    avgRecA = tpA / float(rowSumA)

  # Calculate the precision and recall of B
  tpB = npMatrix[1, 1]
  colSumB = npMatrix[:, 1].sum(0)
  rowSumB = npMatrix[1, :].sum(1)

  if tpB != 0 and colSumB != 0:
    avgPrecB = tpB / float(colSumB)

  if tpB != 0 and rowSumB != 0:
    avgRecB = tpB / float(rowSumB)

  # Calculate the precision and recall of C
  tpC = npMatrix[2, 2]
  colSumC = npMatrix[:, 2].sum(0)
  rowSumC = npMatrix[2, :].sum(1)

  if tpC!= 0 and colSumC!= 0:
    avgPrecC= tpC/ float(colSumC)

  if tpC!= 0 and rowSumC!= 0:
    avgRecC= tpC/ float(rowSumC)

  # Calculate the precision and recall of D
  tpD = npMatrix[3, 3]
  colSumD = npMatrix[:, 3].sum(0)
  rowSumD = npMatrix[3, :].sum(1)

  if tpD != 0 and colSumD != 0:
    avgPrecD = tpD / float(colSumD)

  if tpD != 0 and rowSumD != 0:
    avgRecD = tpD / float(rowSumD)

  return [
    acc,
    avgPrecA, avgRecA,
    avgPrecB, avgRecB,
    avgPrecC, avgRecC,
    avgPrecD, avgRecD
  ]

# Train and evaluate the classifier
def _train_and_evaluate_classifier(classifier):
  acc = 0
  avgPrecA = avgRecA = 0
  avgPrecB = avgRecB = 0
  avgPrecC = avgRecC = 0
  avgPrecD = avgRecD = 0

  for i, (train_indexes, test_indexes) in enumerate(cv):
    X_train = X[train_indexes, :]
    y_train = y[train_indexes]
    X_test = X[test_indexes, :]
    y_test = y[test_indexes]
    classifier.fit(X_train, y_train)

    y_pred = classifier.predict(X_test)
    conf = confusion_matrix(y_test, y_pred, labels = [0, 1, 2, 3])

    # Add up the accuracies, precisions, and recalls
    vals = _compute_values(conf)
    acc += vals[0]
    avgPrecA += vals[1]
    avgRecA += vals[2]
    avgPrecB += vals[3]
    avgRecB += vals[4]
    avgPrecC += vals[5]
    avgRecC += vals[6]
    avgPrecD += vals[7]
    avgRecD += vals[8]

    print("Fold {}".format(i))

  # Print the calculated averages
  print "Average accuracy:", acc / 10
  print "Average precision:", avgPrecA / 10, avgPrecB / 10, avgPrecC / 10, avgPrecD / 10
  print "Average recall:", avgRecA / 10, avgRecB / 10, avgRecC / 10, avgRecD / 10

# tree = DecisionTreeClassifier(criterion = "entropy", max_depth = 3)
# _train_and_evaluate_classifier(tree)
# export_graphviz(tree, out_file = "tree1.dot", feature_names = feature_names)

# tree = DecisionTreeClassifier(criterion = "entropy", max_depth = 2, max_features = 15)
# _train_and_evaluate_classifier(tree)
# export_graphviz(tree, out_file = "tree2.dot", feature_names = feature_names)

# tree = DecisionTreeClassifier(criterion = "entropy", max_depth = 4, max_features = 20)
# _train_and_evaluate_classifier(tree)
# export_graphviz(tree, out_file = "tree3.dot", feature_names = feature_names)

tree = DecisionTreeClassifier(criterion = "entropy", max_depth = 5, max_features = 30)
_train_and_evaluate_classifier(tree)
# export_graphviz(tree, out_file = "tree4.dot", feature_names = feature_names)

# tree = DecisionTreeClassifier(criterion = "entropy", max_depth = 6, max_features = 5)
# _train_and_evaluate_classifier(tree)
# export_graphviz(tree, out_file = "tree5.dot", feature_names = feature_names)

# TODO: Evaluate another classifier, i.e. SVM, Logistic Regression, k-NN, etc.
# clf = svm.LinearSVC(C = 4.0)
# _train_and_evaluate_classifier(clf)

# TODO: Once you have collected data, train your best model on the entire
# dataset. Then save it to disk as follows:

# when ready, set this to the best model you found, trained on all the data:
best_classifier = tree
with open("classifier.pickle", "wb") as f: # "wb" stands for "write bytes"
  pickle.dump(best_classifier, f)