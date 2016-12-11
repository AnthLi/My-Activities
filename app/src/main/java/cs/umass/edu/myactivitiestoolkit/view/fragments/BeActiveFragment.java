package cs.umass.edu.myactivitiestoolkit.view.fragments;

import android.app.Dialog;
import android.app.Fragment;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.androidplot.pie.PieChart;
import com.androidplot.pie.PieRenderer;
import com.androidplot.pie.Segment;
import com.androidplot.pie.SegmentFormatter;
import com.androidplot.util.PixelUtils;

import cs.umass.edu.myactivitiestoolkit.R;
import cs.umass.edu.myactivitiestoolkit.constants.Constants;
import cs.umass.edu.myactivitiestoolkit.services.BeActiveService;
import cs.umass.edu.myactivitiestoolkit.services.ServiceManager;
import cs.umass.edu.myactivitiestoolkit.view.activities.MainActivity;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;


public class BeActiveFragment extends Fragment {

  @SuppressWarnings("unused")
  private static final String TAG = ExerciseFragment.class.getName();

  private static final int SELECTED_SEGMENT_OFFSET = 50;

  private ServiceManager mServiceManager;

  private View activityIcon;

  private TextView textActivity;

  private Switch switchBeActive;

  // Pie chart to display the ratio between sedentary and active
  private PieChart pieChart;

  private TextView pieChartSizeTextView;

  private SeekBar pieChartSizeSeekBar;

  // List containing the timestamps associated with sitting
  private ArrayList<Long> sedentaryTimestamps = new ArrayList<>();

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
              activityIcon.setBackgroundResource(R.drawable.ic_sitting_black_48dp);
              textActivity.setText(R.string.be_active_sedentary);

              ++sedentaryCount;

              // If the user has been sedentary long enough, display a
              // notification telling them to move around a bit
              if (sedentaryTimestamps.size() > 0) {
                Long start = sedentaryTimestamps.get(0);

                if (timestamp - start >= 3 * 1000) {
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
              activityIcon.setBackgroundResource(R.drawable.ic_running_black_48dp);
              textActivity.setText(R.string.be_active_active);

              ++activeCount;

              // Now that the user is active, clear the list of sedentary
              // timestamps for the next time they're sedentary
              sedentaryTimestamps.clear();

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

    switchBeActive.setChecked(mServiceManager.isServiceRunning(BeActiveService.class));
    switchBeActive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
        if (enabled) {
          clearPlotData();
          mServiceManager.startSensorService(BeActiveService.class);
        }
        else {
          activityIcon.setBackgroundResource(R.drawable.ic_more_horiz_black_48dp);
          textActivity.setText(R.string.be_active_initial);
          mServiceManager.stopSensorService(BeActiveService.class);
        }
      }
    });

    final float padding = PixelUtils.dpToPix(30);
    pieChart.getPie().setPadding(padding, padding, padding, padding);

    // detect segment clicks:
    pieChart.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        PointF click = new PointF(motionEvent.getX(), motionEvent.getY());
        if (pieChart.getPie().containsPoint(click)) {
          Segment segment = pieChart.getRenderer(PieRenderer.class).getContainingSegment(click);
          final boolean isSelected = getFormatter(segment).getOffset() != 0;
          deselectAll();
          setSelected(segment, !isSelected);
          pieChart.redraw();
        }

        return false;
      }

      private SegmentFormatter getFormatter(Segment segment) {
        return pieChart.getFormatter(segment, PieRenderer.class);
      }

      private void deselectAll() {
        List<Segment> segments = pieChart.getRegistry().getSeriesList();

        for (Segment segment : segments) {
          setSelected(segment, false);
        }
      }

      private void setSelected(Segment segment, boolean isSelected) {
        SegmentFormatter f = getFormatter(segment);

        if (isSelected) {
          f.setOffset(SELECTED_SEGMENT_OFFSET);
        }
        else {
          f.setOffset(0);
        }
      }
    });

    pieChartSizeSeekBar = (SeekBar)view.findViewById(R.id.pieChartSizeSeekBar);
    pieChartSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        pieChart.getRenderer(PieRenderer.class).setDonutSize(
          seekBar.getProgress() / 100f,
          PieRenderer.DonutMode.PERCENT
        );

        pieChart.redraw();
        updatePieChartText();
      }
    });

    pieChartSizeTextView = (TextView)view.findViewById(R.id.pieChartSizeTextView);
    updatePieChartText();

    Segment s1 = new Segment("s1", 3);
    Segment s2 = new Segment("s2", 1);
    Segment s3 = new Segment("s3", 7);
    Segment s4 = new Segment("s4", 9);

    EmbossMaskFilter emf = new EmbossMaskFilter(new float[]{1, 1, 1}, 0.4f, 10, 8.2f);

    SegmentFormatter sf1 = new SegmentFormatter(R.xml.pie_segment_formatter1);
    sf1.getLabelPaint().setShadowLayer(3, 0, 0, Color.BLACK);
    sf1.getFillPaint().setMaskFilter(emf);

    SegmentFormatter sf2 = new SegmentFormatter(R.xml.pie_segment_formatter2);
    sf2.getLabelPaint().setShadowLayer(3, 0, 0, Color.BLACK);
    sf2.getFillPaint().setMaskFilter(emf);

    SegmentFormatter sf3 = new SegmentFormatter(R.xml.pie_segment_formatter3);
    sf3.getLabelPaint().setShadowLayer(3, 0, 0, Color.BLACK);
    sf3.getFillPaint().setMaskFilter(emf);

    SegmentFormatter sf4 = new SegmentFormatter(R.xml.pie_segment_formatter4);
    sf4.getLabelPaint().setShadowLayer(3, 0, 0, Color.BLACK);
    sf4.getFillPaint().setMaskFilter(emf);

    pieChart.addSegment(s1, sf1);
    pieChart.addSegment(s2, sf2);
    pieChart.addSegment(s3, sf3);
    pieChart.addSegment(s4, sf4);

    pieChart.getBorderPaint().setColor(Color.TRANSPARENT);
    pieChart.getBackgroundPaint().setColor(Color.TRANSPARENT);

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

  private void updatePieChart() {

  }

  private void updatePieChartText() {
    String text = pieChartSizeSeekBar.getProgress() + "%";
    pieChartSizeTextView.setText(text);
  }

  private void clearPlotData() {
    //    pieChart.clear();
  }
}
