package cs.umass.edu.myactivitiestoolkit.services;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.processing.Filter;
import edu.umass.cs.MHLClient.client.MessageReceiver;
import edu.umass.cs.MHLClient.sensors.AccelerometerReading;

public class BeActiveService extends SensorService implements SensorEventListener {
  private static final String TAG = AccelerometerService.class.getName();
  private SensorManager mSensorManager;
  private Sensor mAccelerometerSensor;

  public BeActiveService() {System.out.print("starting");}

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
    Log.i(TAG, "Accuracy changed: " + accuracy);
  }

  @Override
  protected void onServiceStarted() {
    broadcastMessage(Constants.MESSAGE.BE_ACTIVE_SERVICE_STARTED);
  }

  @Override
  protected void onServiceStopped() {
    broadcastMessage(Constants.MESSAGE.BE_ACTIVE_SERVICE_STOPPED);
  }

  @Override
  public void onConnected() {
    super.onConnected();

    mClient.registerMessageReceiver(new MessageReceiver(Constants.MHLClientFilter.ACTIVITY_DETECTED) {
      @Override
      protected void onMessageReceived(JSONObject json) {
        try {
          JSONObject data = json.getJSONObject("data");
          String activity = data.getString("activity");

          // Differentiate between A2 and the final project by checking for a
          // timestamp associated with the JSON data
          if (!data.isNull("timestamp")) {
            long timestamp = data.getLong("timestamp");
            broadcastBeActiveDetected(activity, timestamp);
          }
        }
        catch (JSONException e) {
          e.printStackTrace();
        }
      }
    });
  }

  @Override
  protected void registerSensors() {
    // Register the accelerometer sensor from the sensor manager.
    mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

    mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    mSensorManager.registerListener(
      this,
      mAccelerometerSensor,
      SensorManager.SENSOR_DELAY_NORMAL
    );
  }

  @Override
  protected void unregisterSensors() {
    // Make sure mSensorManager is not null before unregistering all listeners
    if (mSensorManager != null) {
      mSensorManager.unregisterListener(this, mAccelerometerSensor);
    }
  }

  @Override
  protected int getNotificationID() {
    return Constants.NOTIFICATION_ID.BE_ACTIVE_SERVICE;
  }

  @Override
  protected String getNotificationContentText() {
    return getString(R.string.activity_service_notification);
  }

  @Override
  protected int getNotificationIconResourceID() {
    return R.drawable.ic_running_white_24dp;
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
      // convert the timestamp to milliseconds (note this is not in Unix time)
      long timestamp_in_milliseconds = (long)((double)event.timestamp / Constants.TIMESTAMPS.NANOSECONDS_PER_MILLISECOND);

      // Filter the event values
      Filter filter = new Filter(1);
      double[] filteredValues = filter.getFilteredValues(event.values);
      float[] filteredFloatValues = new float[filteredValues.length];

      for (int i = 0; i < filteredValues.length; i++) {
        filteredFloatValues[i] = (float)filteredValues[i];
      }

      mClient.sendSensorReading(new AccelerometerReading(
        mUserID,
        "MOBILE",
        "",
        timestamp_in_milliseconds,
        0,
        filteredFloatValues
      ));
    }
    else {
      // cannot identify sensor type
      Log.w(TAG, Constants.ERROR_MESSAGES.WARNING_SENSOR_NOT_SUPPORTED);
    }
  }

  // Broadcast the current activity and timestamp to the Be Active UI
  private void broadcastBeActiveDetected(String activity, long timestamp) {
    Intent intent = new Intent();
    intent.putExtra(Constants.KEY.BE_ACTIVE_ACTIVITY, activity);
    intent.putExtra(Constants.KEY.BE_ACTIVE_TIMESTAMP, timestamp);
    intent.setAction(Constants.ACTION.BROADCAST_BE_ACTIVE);
    LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
    manager.sendBroadcast(intent);
  }
}
