package com.example.project;

import static com.example.project.JsonHelper.getJSONObjectFromURL;

import android.graphics.Color;
import android.os.AsyncTask;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IRadarDataSet;
import com.github.niqdev.mjpeg.DisplayMode;
import com.github.niqdev.mjpeg.Mjpeg;
import com.github.niqdev.mjpeg.MjpegView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    MjpegView mjpegView;
    ViewFlipper viewFlipper;
    TextView statusText, text_camera, text_chart, welcome_text;
    EditText sett_text;
    String ip = "192.168.1.111";
    private RadarChart chartRadar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        viewFlipper = findViewById(R.id.viewflipper);
        text_chart = findViewById(R.id.chart_emotion);
        text_camera = findViewById(R.id.camera_emotion);
        mjpegView = findViewById(R.id.mjpegViewDefault);
        statusText = findViewById(R.id.status_text);
        sett_text = findViewById(R.id.ip);
        welcome_text = findViewById(R.id.welcome_status);
        chartRadar = findViewById(R.id.chart_radar);

        viewFlipper.setDisplayedChild(2);
        int TIMEOUT = 3600;
        Mjpeg.newInstance().open(String.format("http://%s:56000/stream", ip), TIMEOUT).subscribe(inputStream -> {
                    mjpegView.setSource(inputStream);
                    mjpegView.setRotate(180);
                    mjpegView.setDisplayMode(DisplayMode.BEST_FIT);
                    mjpegView.showFps(true);
                });

        new FetchData().execute();

        sett_text.setText(ip);
        viewFlipper.setDisplayedChild(5);

        Button apply = findViewById(R.id.settings_apply);
        Button buttonWelcome = findViewById(R.id.welcome_continue);
        ImageButton button_statistic = findViewById(R.id.button1);
        ImageButton button_camera = findViewById(R.id.button2);
        ImageButton button_emotions = findViewById(R.id.button3);
        ImageButton button_instruction = findViewById(R.id.button4);
        ImageButton button_settings = findViewById(R.id.stat);
        ImageButton neutral = findViewById(R.id.neutral);
        ImageButton happy = findViewById(R.id.happy);
        ImageButton fear = findViewById(R.id.fear);
        ImageButton sad = findViewById(R.id.sad);
        ImageButton suprised = findViewById(R.id.suprised);
        ImageButton angry = findViewById(R.id.angry);

        apply.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ip = sett_text.getText().toString();
            }
        });

        buttonWelcome.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                viewFlipper.setDisplayedChild(1);
            }
        });


        button_statistic.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                viewFlipper.setDisplayedChild(0);
            }
        });
        button_camera.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                viewFlipper.setDisplayedChild(1);
            }
        });
        button_emotions.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                viewFlipper.setDisplayedChild(2);
            }
        });
        button_instruction.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                viewFlipper.setDisplayedChild(3);
            }
        });
        button_settings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                viewFlipper.setDisplayedChild(4);
            }
        });


        neutral.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendData sendData = new SendData();
                sendData.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "6");
            }
        });
        happy.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendData sendData = new SendData();
                sendData.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "3");            }
        });
        fear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendData sendData = new SendData();
                sendData.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "2");            }
        });
        sad.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendData sendData = new SendData();
                sendData.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "4");            }
        });
        suprised.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendData sendData = new SendData();
                sendData.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "5");            }
        });
        angry.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendData sendData = new SendData();
                sendData.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "0");            }
        });
    }

    class FetchData extends AsyncTask<String, JSONObject, Boolean> {

        @Override
        protected Boolean doInBackground(final String... args) {
            while (true) {
                try {
                    JSONObject jsonObject = getJSONObjectFromURL(String.format("http://%s:5000/stats", ip));
                    super.publishProgress(jsonObject);
                    Thread.sleep(1000);
                } catch (Exception e) {}
            }
        }

        @Override
        protected void onProgressUpdate(JSONObject... value) {

            MainActivity.this.statusText.setText("подключено");
            MainActivity.this.welcome_text.setText("Подключено");

            try {
                MainActivity.this.text_chart.setText(value[0].getString("lastEmotionName"));
                MainActivity.this.text_camera.setText(value[0].getString("lastEmotionName"));

            } catch (JSONException e) {}

            try {
                setDataRadar(value[0]);
            } catch (JSONException e) {}
        }

        private String[] emotions = new String[]{
                "Злость",
                "Отвращение",
                "Страх",
                "Счастье",
                "Печаль",
                "Удивление",
                "Нейтральный"
        };

        private void setDataRadar(JSONObject rawData) throws JSONException {

            JSONArray stats = rawData.getJSONArray("stats");
            ArrayList<RadarEntry> entries = new ArrayList<>();

            for (int i = 0; i < emotions.length - 1; i++) {
                JSONObject stat = stats.getJSONObject(i);
                int count = stat.getInt("count");
                entries.add(new RadarEntry(count));
            }

            RadarDataSet set = new RadarDataSet(entries, "Эмоциональное состояние");

            set.setColor(Color.rgb(0, 0, 0));
            set.setFillColor(Color.rgb(198, 226, 192));
            set.setDrawFilled(true);
            set.setFillAlpha(180);
            set.setLineWidth(1f);
            set.setDrawHighlightCircleEnabled(true);
            set.setDrawHighlightIndicators(false);

            ArrayList<IRadarDataSet> sets = new ArrayList<>();
            sets.add(set);

            RadarData data = new RadarData(sets);

            chartRadar.setData(data);
            chartRadar.invalidate();

            chartRadar.getXAxis().setValueFormatter(new IndexAxisValueFormatter(emotions));
            chartRadar.getXAxis().setTextColor(Color.rgb(0, 0, 0));
            chartRadar.getXAxis().setTextSize(13);
            chartRadar.getDescription().setEnabled(false);
        }
    }

    class SendData extends AsyncTask<String, JSONObject, Boolean> {
        @Override
        protected Boolean doInBackground(final String... args) {
            try {
                JSONObject jsonObject = getJSONObjectFromURL(String.format("http://%s:5000/emotion/%s", ip, args[0]));
                super.publishProgress(jsonObject);

            } catch (Exception e) { return false; }

            return true;
        }
    }
}
