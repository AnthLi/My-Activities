package cs.umass.edu.myactivitiestoolkit.view.fragments;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.androidplot.pie.PieChart;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.RunnableFuture;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.services.msband.BandService;
import cs.umass.edu.myactivitiestoolkit.services.AccelerometerService;
import cs.umass.edu.myactivitiestoolkit.services.ServiceManager;

/**
 * Created by anthonyli on 12/7/16.
 */
public class BeActiveFragment extends Fragment {

  @SuppressWarnings("unused")
  private static final String TAG = ExerciseFragment.class.getName();

  private Switch switchAccelerometer;

  private TextView txtActivity;

  private PieChart pieChart;

  private List<Integer> sittingMinutes = new ArrayList<>();

  private List<Integer> activeMinutes = new ArrayList<>();

  private ServiceManager mServiceManager;

  private static BeActiveFragment ourInstance = new BeActiveFragment();

  public static BeActiveFragment getInstance() {
    return ourInstance;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mServiceManager = ServiceManager.getInstance(getActivity());
  }

  @Override
  public View onCreateView(
    LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
  ) {
    final View view = inflater.inflate(R.layout.fragment_be_active, container, false);

    txtActivity = (TextView)view.findViewById(R.id.txtActivity);

    switchAccelerometer = (Switch)view.findViewById(R.id.switchAccelerometer);
    switchAccelerometer.setChecked(mServiceManager.isServiceRunning(AccelerometerService.class));
    switchAccelerometer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
        if (enabled) {
          clearPlotData();
          mServiceManager.startSensorService(AccelerometerService.class);
        }
        else {
          mServiceManager.stopSensorService(AccelerometerService.class);
        }
      }
    });

    return view;
  }

  @Override
  public void onStart() {

  }

  @Override
  public void onStop() {

  }

  private void updatePlot() {

  }

  private void clearPlotData() {
    pieChart.clear();
  }
}
