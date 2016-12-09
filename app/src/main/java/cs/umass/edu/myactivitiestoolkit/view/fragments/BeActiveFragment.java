package cs.umass.edu.myactivitiestoolkit.view.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.androidplot.pie.PieChart;

import java.util.ArrayList;
import java.util.List;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.services.BeActiveService;
import cs.umass.edu.myactivitiestoolkit.services.ServiceManager;

public class BeActiveFragment extends Fragment {

  @SuppressWarnings("unused")
  private static final String TAG = ExerciseFragment.class.getName();

  private Switch switchBeActive;

  // Text view used to display the current activity performed by the user
  private TextView txtActivity;

  // Pie chart to display the ratio between sedentary and active
  private PieChart pieChart;

  // List containing the timestamps associated with sitting
  private List<Long> sittingTimestamps = new ArrayList<>();

  // List containing the timestamps associated with being active
  private List<Long> activeTimestamps = new ArrayList<>();

  private ServiceManager mServiceManager;

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

          if (activity.equals("active")) {
            sittingTimestamps.add(timestamp);
          }
          else if (activity.equals("sedentary")) {
            activeTimestamps.add(timestamp);
          }

          displayActivity(activity);

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
    final View view = inflater.inflate(R.layout.fragment_be_active, container, false);

    txtActivity = (TextView)view.findViewById(R.id.txtActivity);

    switchBeActive = (Switch)view.findViewById(R.id.switchBeActive);
    switchBeActive.setChecked(mServiceManager.isServiceRunning(BeActiveService.class));
    switchBeActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
        if (enabled) {
          clearPlotData();
          mServiceManager.startSensorService(BeActiveService.class);
        }
        else {
          mServiceManager.stopSensorService(BeActiveService.class);
        }
      }
    });
//    String myHtml = "HTML CAN BE USED IF NECESSARY";
//    txtActivity.setText(Html.fromHtml(myHtml));
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
    getActivity().runOnUiThread(new Runnable() {
      @Override
      public void run() {
        // txtActivity.setText(activity);
      }
    });
  }

  private void updatePieChart() {

  }

  private void clearPlotData() {
//    pieChart.clear();
  }
}
