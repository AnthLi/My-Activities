package cs.umass.edu.myactivitiestoolkit.steps;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;

import cs.umass.edu.myactivitiestoolkit.processing.Filter;

class Calculation {
  float min;
  float max;

  Calculation(float min, float max) {
    this.min = min;
    this.max = max;
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

  // Window of sample values used for step detection
  private ArrayList<Float> samples = new ArrayList<>();

  // Return the axis with the maximum acceleration. 0 is the X axis, 1 is the
  // Y axis, and 2 is the Z axis
  private static int getMaxAxis(float[] values) {
    float max = 0f;
    int axis = 0;

    for (int i = 0; i < values.length; i++) {
      float currMax = Math.abs(values[i]);

      if (currMax > max) {
        max = currMax;
        axis = i;
      }
    }

    return axis;
  }

  // Calculate the min and max values.
  private static Calculation calculate(float[] values) {
    float min = 0f;
    float max = 0f;

    for (float v : values) {
      if (v > max) {
        max = v;
      }

      if (v < min) {
        min = v;
      }
    }

    return new Calculation(min, max);
  }

  // Step Detection Algorithm using Dynamic Detection Threshold
  private boolean stepDetection(float[] values) {
    final int windowSize = 10;
    boolean stepDetected = false;
    // Axis with max acceleration
    int axis = getMaxAxis(values);

    // Add enough values to fill the window size
    if (samples.size() < windowSize) {
      samples.add(values[axis]);
    }
    else {
      // Retrieve the min and max values
      Calculation calculation = calculate(values);
      // Determine the dynamic detection threshold with the min and max values
      float threshold = Math.abs((calculation.max + calculation.min) / 2);

      for (int i = 1; i < samples.size(); i++) {
        float prevSample = Math.abs(samples.get(i - 1));
        float currSample = Math.abs(samples.get(i));

        if (!stepDetected) {
          if (currSample < threshold && currSample < prevSample) {
            stepDetected = true;

            // Reset the sample data to retrieve the next set of values
            samples.clear();
          }
        }
      }
    }

    return stepDetected;
  }

  /**
   * Here is where you will receive accelerometer readings, buffer them if
   * necessary and run your step detection algorithm. When a step is detected,
   * call {@link #onStepDetected(long, float[])} to notify all listeners.
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
      filter = new Filter(1);
      double[] filteredValues = filter.getFilteredValues(event.values);
      float[] filteredFloatValues = new float[filteredValues.length];

      for (int i = 0; i < filteredValues.length; i++) {
        filteredFloatValues[i] = (float)filteredValues[i];
      }

      if (stepDetection(filteredFloatValues)) {
        onStepDetected(event.timestamp, filteredFloatValues);
      }
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
