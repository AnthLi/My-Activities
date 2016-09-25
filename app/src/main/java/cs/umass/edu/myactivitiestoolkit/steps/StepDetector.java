package cs.umass.edu.myactivitiestoolkit.steps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.util.ArrayList;

import cs.umass.edu.myactivitiestoolkit.processing.Filter;

class EventTuple {
  public long timeStamp;
  public float[] values;

  public EventTuple(long t, float[] v){
    timeStamp = t;
    values = v;
  }
}

/**
 * This class is responsible for detecting steps from the accelerometer sensor.
 * All {@link OnStepListener step listeners} that have been registered will
 * be notified when a step is detected.
 */
public class StepDetector implements SensorEventListener {
  /**
   * Used for debugging purposes.
   */
  @SuppressWarnings("unused")
  private static final String TAG = StepDetector.class.getName();

  /**
   * Maintains the set of listeners registered to handle step events.
   **/
  private ArrayList<OnStepListener> mStepListeners;

  /**
   * The number of steps taken.
   */
  private int stepCount;

  private Filter filter;

  public StepDetector() {
    mStepListeners = new ArrayList<>();
    stepCount = 0;
  }

  /**
   * Registers a step listener for handling step events.
   *
   * @param stepListener defines how step events are handled.
   */
  public void registerOnStepListener(final OnStepListener stepListener) {
    mStepListeners.add(stepListener);
  }

  /**
   * Unregisters the specified step listener.
   *
   * @param stepListener the listener to be unregistered. It must already be registered.
   */
  public void unregisterOnStepListener(final OnStepListener stepListener) {
    mStepListeners.remove(stepListener);
  }

  /**
   * Unregisters all step listeners.
   */
  public void unregisterOnStepListeners() {
    mStepListeners.clear();
  }

  // Size of data to collect for step detection
  private final int window = 20;
  private ArrayList<EventTuple> sampleData = new ArrayList<EventTuple>();

  private boolean stepDetected(EventTuple event) {
    if (sampleData.size() < window) {
      sampleData.add(event);
    }
    else {
      //if the array has hit out desired size of the sample window...
      //detect movement by change of z value
      float base = 9.8f;
      float offset = 4f;
      boolean dipBelow = false;
      boolean dipAbove = false;
      //if first data point is greater base line
      if (sampleData.get(0).values[2] >= base) {
        dipAbove = true;
      }
      //if the first data point is beneath the base
      else {
        dipBelow = true;
      }

      int i = 0;
      while (i < sampleData.size()) {
        if (dipAbove) {
          //should have already broadcast, so we are waiting for the trend to go back down
          if (sampleData.get(i).values[2] < base+offset) {
            Log.d(TAG, String.valueOf(sampleData.get(i).values[2]));
            dipBelow = true;
            dipAbove = false;
          }
        }
        else {
          if (sampleData.get(i).values[2] > base + offset) {
            Log.d(TAG, String.valueOf(sampleData.get(i).values[2]));
            dipAbove = true;
            dipBelow = false;
          }
        }

        i++;
      }

      sampleData = new ArrayList<EventTuple>();
    }

    return false;
  }

  /**
   * Here is where you will receive accelerometer readings, buffer them if necessary
   * and run your step detection algorithm. When a step is detected, call
   * {@link #onStepDetected(long, float[])} to notify all listeners.
   * <p>
   * Recall that human steps tend to take anywhere between 0.5 and 2 seconds.
   *
   * @param event sensor reading
   */
  @Override
  public void onSensorChanged(SensorEvent event) {
    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
      // TODO: Detect steps! Call onStepDetected(...) when a step is detected.

      // Filter the event values
      filter = new Filter(10.0);
      double[] filteredValues = filter.getFilteredValues(event.values);
      float[] filteredFloatValues = new float[filteredValues.length];

      for (int i = 0; i < filteredValues.length; i++) {
        filteredFloatValues[i] = (float)filteredValues[i];
      }

//      if (stepDetected(new EventTuple(event.timestamp, filteredFloatValues))) {
//        onStepDetected(event.timestamp, filteredFloatValues);
//      }

      Log.d(
        TAG,
        "X: " + event.values[0] +
          ", Y: " + event.values[1] +
          ", Z: " + event.values[2]
      );
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {
    // do nothing
  }

  /**
   * This method is called when a step is detected. It updates the current step count,
   * notifies all listeners that a step has occurred and also notifies all listeners
   * of the current step count.
   */
  private void onStepDetected(long timestamp, float[] values) {
    stepCount++;
    for (OnStepListener stepListener : mStepListeners) {
      stepListener.onStepDetected(timestamp, values);
      stepListener.onStepCountUpdated(stepCount);
    }
  }
}
