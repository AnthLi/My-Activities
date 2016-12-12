package cs.umass.edu.myactivitiestoolkit.view.fragments;

import android.app.Fragment;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.services.BeActiveService;
import cs.umass.edu.myactivitiestoolkit.services.ServiceManager;

import java.util.ArrayList;

import static android.content.Context.NOTIFICATION_SERVICE;


public class BeActiveFragment extends Fragment {

  @SuppressWarnings("unused")
  private static final String TAG = ExerciseFragment.class.getName();

  private ServiceManager mServiceManager;

  private View activityIcon;

  private TextView textActivity;

  private Switch switchBeActive;

  // Pie chart to display the ratio between sedentary and active
  private PieChart pieChart;

  private ArrayList<PieEntry> entries;

  // List containing the timestamps associated with sitting
  private ArrayList<Long> sedentaryTimestamps = new ArrayList<>();
  private ArrayList<Long> activeTimestamps = new ArrayList<>();

  private int sedentaryCount = 0;

  private int activeCount = 0;

  private final BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction() == null) {
        return;
      }

      switch (intent.getAction()) {
        case Constants.ACTION.BROADCAST_MESSAGE:
          int message = intent.getIntExtra(Constants.KEY.MESSAGE, -1);

          // Turn the switch off when the service has stopped
          if (message == Constants.MESSAGE.BE_ACTIVE_SERVICE_STOPPED) {
            switchBeActive.setChecked(false);
          }

          break;

        // Display the current activity and
        case Constants.ACTION.BROADCAST_BE_ACTIVE:
          String activity = intent.getStringExtra(Constants.KEY.BE_ACTIVE_ACTIVITY);
          long timestamp = intent.getLongExtra(Constants.KEY.BE_ACTIVE_TIMESTAMP, -1);

          displayActivity(activity);

          // Update the icon and current activity on the UI
          switch (activity) {
            case "Sedentary":

              //if there has been no activity for 5 seconds or more, clear active and focus on sitting
              if((System.currentTimeMillis() - activeTimestamps.get(activeTimestamps.size()-1)) >= 2000) {
                activeTimestamps.clear();
              }

              activityIcon.setBackgroundResource(R.drawable.ic_sitting_black_48dp);
              textActivity.setText(R.string.be_active_sedentary);

              updatePieChartData(activity, ++sedentaryCount);

              // If the user has been sedentary long enough, display a
              // notification telling them to move around a bit
              if (sedentaryTimestamps.size() > 0) {
                Long start = sedentaryTimestamps.get(0);

                if (timestamp - start >= 60 * 1000) {
                  NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_sitting_white_48dp)
                    .setContentTitle("Be Active!")
                    .setContentText("Go move around a bit before sitting back down!");

                  Intent resIntent = new Intent(context, BeActiveService.class);
                  mBuilder.setContentIntent(PendingIntent.getActivity(
                    context,
                    0,
                    resIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                  ));

                  NotificationManager mNotifyMgr = (NotificationManager)context
                    .getSystemService(NOTIFICATION_SERVICE);

                  mNotifyMgr.notify(1, mBuilder.build());
                }
              }
              else {
                sedentaryTimestamps.add(timestamp);
              }

              break;

            case "Active":

              if(activeTimestamps.size() != 0) {
                activityIcon.setBackgroundResource(R.drawable.ic_running_black_48dp);
                textActivity.setText(R.string.be_active_active);
                
                updatePieChartData(activity, ++activeCount);

                // Now that the user is active, clear the list of sedentary
                // timestamps for the next time they're sedentary
                sedentaryTimestamps.clear();
              }

              activeTimestamps.add(System.currentTimeMillis());

              break;
          }

          break;
      }
    }
  };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mServiceManager = ServiceManager.getInstance(getActivity());
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final View view = inflater.inflate(R.layout.fragment_be_active, container, false);

    activityIcon = view.findViewById(R.id.activityIcon);
    textActivity = (TextView)view.findViewById(R.id.textActivity);
    switchBeActive = (Switch)view.findViewById(R.id.switchBeActive);
    pieChart = (PieChart)view.findViewById(R.id.beActivePieChart);

    // Configure the switch
    switchBeActive.setChecked(mServiceManager.isServiceRunning(BeActiveService.class));
    switchBeActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
        if (enabled) {
          clearData();
          mServiceManager.startSensorService(BeActiveService.class);
        }
        else {
          activityIcon.setBackgroundResource(R.drawable.ic_more_horiz_black_48dp);
          textActivity.setText(R.string.be_active_initial);
          mServiceManager.stopSensorService(BeActiveService.class);
        }
      }
    });

    // Configure the pie chart
    pieChart.setUsePercentValues(false);
    pieChart.getDescription().setEnabled(false);
    pieChart.setDrawHoleEnabled(true);
    pieChart.setHoleColor(Color.TRANSPARENT);
    pieChart.setHoleRadius(50);
    pieChart.setTransparentCircleRadius(10);
    pieChart.setRotationAngle(0);
    pieChart.setRotationEnabled(true);

    // Add data to the pie chart
    entries = new ArrayList<>();
    entries.add(new PieEntry(0f, "Sedentary"));
    entries.add(new PieEntry(0f, "Active"));

    // Generate the data set for the pie chart
    PieDataSet dataSet = new PieDataSet(entries, "");
    dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
    dataSet.setDrawValues(false);

    pieChart.setData(new PieData(dataSet));

    // Set a chart value selected listener
    pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
      @Override
      public void onValueSelected(Entry e, Highlight h) {
        if (e == null) {
          return;
        }

        // Calculate the percentage of the selected activity
        String label = ((PieEntry)e).getLabel();
        float value = ((PieEntry)e).getValue();
        int total = sedentaryCount + activeCount;
        float percentage = Math.round((value / total) * 100);

        // Display a toast with the activity name and percentage
        Context context = getActivity().getApplicationContext();
        Toast.makeText(
          context,
          label + ": " + percentage + "%",
          Toast.LENGTH_SHORT
        ).show();
      }

      @Override
      public void onNothingSelected() {}
    });

    return view;
  }

  @Override
  public void onStart() {
    super.onStart();

    LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
    IntentFilter filter = new IntentFilter();
    filter.addAction(Constants.ACTION.BROADCAST_MESSAGE);
    filter.addAction(Constants.ACTION.BROADCAST_BE_ACTIVE);
    broadcastManager.registerReceiver(receiver, filter);
  }

  @Override
  public void onStop() {
    super.onStop();
  }

  private void displayActivity(final String activity) {
    if (getActivity() == null) {
      return;
    }

    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
         textActivity.setText(activity);
      }
    });
  }

  private void updatePieChartData(String label, Number number) {
    for (int i = 0; i < entries.size(); i++) {
      String entryLabel = entries.get(i).getLabel();

      if (entryLabel.equals(label)) {
        entries.set(i, new PieEntry(number.floatValue(), entryLabel));
      }
    }

    // Update the chart in real-time
    pieChart.invalidate();
    pieChart.notifyDataSetChanged();
  }

  private void clearData() {
    sedentaryCount = 0;
    activeCount = 0;
    sedentaryTimestamps.clear();

    for (int i = 0; i < entries.size(); i++) {
      String entryLabel = entries.get(i).getLabel();
      entries.set(i, new PieEntry(0, entryLabel));
    }
  }
}
