package com.example.project;

import static com.example.project.JsonHelper.getJSONObjectFromURL;

import static java.time.format.FormatStyle.MEDIUM;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Choreographer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.jsibbold.zoomage.ZoomageView;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import xyz.sangcomz.stickytimelineview.TimeLineRecyclerView;
import xyz.sangcomz.stickytimelineview.callback.SectionCallback;
import xyz.sangcomz.stickytimelineview.model.SectionInfo;

public class MainActivity extends AppCompatActivity {
    MjpegView mjpegView;
    ViewFlipper viewFlipper;
    TextView statusText, text_camera, text_chart, welcome_text;
    EditText sett_text;
    String ip = "192.168.1.111";
    private RadarChart chartRadar;
    AppDatabase db;
    TimeLineRecyclerView recyclerView;
    View drawable;
    private EventAdapter timelineAdapter;
    public
    File filesDir;
    LinearLayout layout;
    CardView cardView;
    LinearLayout eventDetails;
    TextView eventDetailsTimeBegin, eventDetailsTimeEnd, eventDetailsDate;
    ZoomageView eventDetailsImage;
    ImageView eventDetailsObject;
    PlayerView playerView;
    public ExoPlayer player;
    Button back_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        Locale locale = new Locale("ru");
        Locale.setDefault(locale);
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                var p = new Intent();
                p.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(p);
            }
        }
        super.onCreate(savedInstanceState);

        try {
            filesDir = getApplicationContext().getExternalFilesDir("BIMO");

            Log.d("files", "каталог " + filesDir.getPath());
            filesDir.mkdirs();

        } catch (Exception ex) {
            Log.e("files", "ошибка создания папки");
        }

        setContentView(R.layout.activity_main);

        text_chart = findViewById(R.id.chart_emotion);
        text_camera = findViewById(R.id.camera_emotion);
        mjpegView = findViewById(R.id.mjpegViewDefault);
        statusText = findViewById(R.id.status_text);
        sett_text = findViewById(R.id.ip);
        welcome_text = findViewById(R.id.welcome_status);
        chartRadar = findViewById(R.id.chart_radar);
        viewFlipper = findViewById(R.id.viewflipper);
        eventDetails = findViewById(R.id.eventDetails);
        eventDetailsImage = findViewById(R.id.eventDetailsImage);
        eventDetailsObject = findViewById(R.id.eventDetailsIcon);
        eventDetailsTimeBegin = findViewById(R.id.eventDetailsTimeBegin);
        eventDetailsTimeEnd = findViewById(R.id.eventDetailsTimeEnd);
        eventDetailsDate = findViewById(R.id.eventDetailsDate);
        playerView = findViewById(R.id.player);
        back_button = findViewById(R.id.button_back);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "events")
                .build();

        new EventGetAllTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        recyclerView = findViewById(R.id.vertical_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                RecyclerView.VERTICAL,
                false));
        timelineAdapter = new EventAdapter(getLayoutInflater(), this, R.layout.recycler_vertical_row);
        recyclerView.addItemDecoration(getSectionCallback(timelineAdapter));
        recyclerView.setAdapter(timelineAdapter);


        int TIMEOUT = 3600;
        Mjpeg.newInstance().open(String.format("http://%s:56000/stream", ip), TIMEOUT).subscribe(inputStream -> {
            mjpegView.setSource(inputStream);
            mjpegView.setRotate(180);
            mjpegView.setDisplayMode(DisplayMode.BEST_FIT);
            mjpegView.showFps(true);
        });


        player = new ExoPlayer.Builder(getApplicationContext()).build();
        playerView.setPlayer(player);


        new FetchData().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        sett_text.setText(ip);
        viewFlipper.setDisplayedChild(5);

        Button apply = findViewById(R.id.settings_apply);
        Button clearAll = findViewById(R.id.clear_all);
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


        back_button.setOnClickListener(v -> {
            viewFlipper.setDisplayedChild(7);
        });

        apply.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ip = sett_text.getText().toString();
            }
        });

        clearAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //db.eventDao().Clear().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
            }
        });

        buttonWelcome.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                viewFlipper.setDisplayedChild(1);
            }
        });


        button_statistic.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                viewFlipper.setDisplayedChild(7);
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
                sendData.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "3");
            }
        });
        fear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendData sendData = new SendData();
                sendData.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "2");
            }
        });
        sad.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendData sendData = new SendData();
                sendData.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "4");
            }
        });
        suprised.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendData sendData = new SendData();
                sendData.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "5");
            }
        });
        angry.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SendData sendData = new SendData();
                sendData.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "0");
            }
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
                } catch (Exception e) {
                    Log.e("network", "FetchData error", e);
                }
            }
        }

        @Override
        protected void onProgressUpdate(JSONObject... value) {

            MainActivity.this.statusText.setText("подключено");
            MainActivity.this.welcome_text.setText("Подключено");

            try {
                MainActivity.this.text_chart.setText(value[0].getString("lastEmotionName"));
                MainActivity.this.text_camera.setText(value[0].getString("lastEmotionName"));

            } catch (JSONException e) {
                Log.e("ui", "Display emotion error", e);
            }

            try {
                setDataRadar(value[0]);
            } catch (JSONException e) {
                Log.e("ui", "radar statistic error", e);
            }
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

            } catch (Exception e) {
                Log.e("network", "FetchData emotions error", e);
                return false;
            }

            return true;
        }
    }

    private Drawable icFinkl, icBuzz, icWannaOne, icGirlsGeneration, icSolo;


    class EventGetAllTask extends AsyncTask<Void, List<Event>, Void> {
        private EventDao eventDao;
        private Date lastUpdated;

        @Override
        protected void onPreExecute() {
            eventDao = db.eventDao();
        }

        @Override
        protected Void doInBackground(final Void... args) {
            //eventDao.Clear();
            lastUpdated = eventDao.getMaxUpdated();

            while (true) {
                try {
                    if (LoadUpdate()) {
                        Thread.sleep(1000);
                    } else {
                        Thread.sleep(10000);
                    }
                } catch (Exception ex) {
                    Log.e("network", "Loading update error", ex);
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        private boolean LoadUpdate() throws IOException, JSONException {
            var timestamp = (long) (lastUpdated.getTime() / 1000.0);
            JSONObject jsonObject = getJSONObjectFromURL(String.format("http://%s:5000/get-update/%d", ip, timestamp));
            JSONArray data = jsonObject.getJSONArray("data");
            if (data.length() == 0) {
                return false;
            }
            List<Event> newEvents = new ArrayList<>();
            for (int i = 0; i < data.length(); i++) {
                var j = data.getJSONObject(i);
                var e = new Event();
                e.id = j.getInt("id");
                e.start = new Date(((long) j.getDouble("start_dt") * 1000));
                if (!j.isNull("end_dt")) e.end = new Date(((long) j.getDouble("end_dt") * 1000));
                if (!j.isNull("updated_dt"))
                    e.updated = new Date(((long) j.getDouble("updated_dt") * 1000));
                if (!j.isNull("duration")) e.duration = j.getInt("duration");
                e.type = j.getString("type");
                e.title = j.getString("title");
                e.object = j.getString("object");
                e.filePhoto = j.getString("file_photo");
                e.fileAudio = j.getString("file_audio");
                e.fileVideo = j.getString("file_video");
                newEvents.add(e);
                downloadFile(e.fileVideo);
                downloadFile(e.fileAudio);
                downloadFile(e.filePhoto);
            }
            Log.d("db", "insert data: " + newEvents.size());
            eventDao.insertAll(newEvents);
            lastUpdated = eventDao.getMaxUpdated();
            return true;
        }

        private void downloadFile(String fileName) {
            if (fileName == "" || fileName == null || fileName == "null") {
                return;
            }
//            Log.d("files", "getFilesDir=" + getApplicationContext().getFilesDir());
//            Log.d("files", "getExternalFilesDir=" + getApplicationContext().getExternalFilesDir(null));
//            Log.d("files", "Environment.getExternalStorageDirectory=" + Environment.getExternalStorageDirectory());
//            Log.d("files", "Environment.getStorageDirectory=" + Environment.getStorageDirectory());
            try {
                var file = new File(filesDir, fileName);
                if (file.exists())
                    return;

                Log.d("files", "Загрузка файла " + fileName);
                Boolean success = HttpDownloadUtility.downloadFile(String.format("http://%s:8080/files/%s", ip, fileName), filesDir.getPath(), fileName);
                Log.d("files", "Успешная загрузка " + fileName);
                Log.d("files", success.toString());

            } catch (IOException e) {
                Log.d("files", "Ошибка загрузки " + fileName);

                throw new RuntimeException(e);
            }
        }

        protected void onProgressUpdate(List<Event>... value) {
            try {

                Log.d("db", "Records in db: " + value[0].size());
//                recyclerView.addItemDecoration(getSectionCallback(value[0]));
//                recyclerView.setAdapter(new EventAdapter(getLayoutInflater(), value[0], R.layout.recycler_vertical_row, R.drawable.emoition_happy));

            } catch (Exception e) {
                throw e;
            }

        }
    }

    public LocalDateTime ToLocalDate(Date dateToConvert) {
        return (
                dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        );
    }

    //private SimpleDateFormat shortDateFormatter = new SimpleDateFormat("EEE, dd MMMM yyyy");
    private DateTimeFormatter shortDateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMMM yyyy", new Locale("ru"));

    private SectionCallback getSectionCallback(final EventAdapter eventListAdapter) {
        return new SectionCallback() {

            @Nullable
            @Override
            public SectionInfo getSectionHeader(int position) {
                var eventList = eventListAdapter.eventList;
                Event event = eventList.get(position);
                Drawable dot = icSolo;
                return new SectionInfo(
                        ToLocalDate(event.start).format(shortDateFormatter),
                        event.type,
                        dot);
            }

            @Override
            public boolean isSection(int position) {
                var eventList = eventListAdapter.eventList;
                var a = ToLocalDate(eventList.get(position).start).truncatedTo(ChronoUnit.DAYS);
                var b = ToLocalDate(eventList.get(position - 1).start).truncatedTo(ChronoUnit.DAYS);
                return false;
            }
        };
    }
}


