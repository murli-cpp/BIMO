package com.example.project;

import androidx.annotation.LayoutRes;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by paetztm on 2/6/2017.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
    private final LayoutInflater layoutInflater;
    private FetchEventsUpdateFromDB fetchEventsUpdateFromDBTask;
    public List<Event> eventList;
    private final int rowLayout;
    private MainActivity mainActivity;
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", new Locale("ru"));



    public EventAdapter(LayoutInflater layoutInflater, MainActivity activity, @LayoutRes int rowLayout) {
        this.layoutInflater = layoutInflater;
        this.eventList = new ArrayList<>();
        this.rowLayout = rowLayout;
        this.mainActivity = activity;
        fetchEventsUpdateFromDBTask = new FetchEventsUpdateFromDB();
        fetchEventsUpdateFromDBTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = layoutInflater.inflate(rowLayout, parent, false);

        ViewHolder viewHolder = new ViewHolder(v);
        v.setOnClickListener(v1 -> {
            Log.d("ui", "card view click:" +( (Event) v1.getTag()).filePhoto);
            Event event = (Event) v1.getTag();
            var file = new File(this.mainActivity.filesDir, event.filePhoto);

            mainActivity.viewFlipper.setDisplayedChild(8);
            mainActivity.eventDetails.setTag(v1.getTag());
            mainActivity.eventDetailsImage.setImageURI(Uri.fromFile(file));
            mainActivity.eventDetailsTimeBegin.setText(ToLocalDate(event.start).format(timeFormatter));
            mainActivity.eventDetailsTimeEnd.setText(ToLocalDate(event.end).format(timeFormatter));
            mainActivity.eventDetailsDate.setText(ToLocalDate(event.start).format(shortDateFormatter));

            int icon = R.drawable.cat;
            switch (event.object) {
                case "cat":
                    icon = R.drawable.cat;
                    break;
                case "dog":
                    icon = R.drawable.dog;
                    break;
                case "person":
                    icon = R.drawable.person;
                    break;
                case "face":
                    icon = R.drawable.face;
                    break;

            }

            mainActivity.eventDetailsObject.setImageResource(icon);


        });

        return viewHolder;
    }


    public LocalDateTime ToLocalDate(Date dateToConvert) {
        return (
                dateToConvert.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        );
    }

    private DateTimeFormatter shortDateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMMM yyyy", new Locale("ru"));


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.id = event.id;
        holder.itemView.setTag(event);
        holder.fullName.setText(event.title);
        holder.date.setText(ToLocalDate(event.start).format(timeFormatter));

        int icon = R.drawable.cat;
        switch (event.object) {
            case "cat":
                icon = R.drawable.cat;
                break;
            case "dog":
                icon = R.drawable.dog;
                break;
            case "person":
                icon = R.drawable.person;
                break;
            case "face":
                icon = R.drawable.face;
                break;

        }
        holder.icon.setImageResource(icon);

        if (event.filePhoto != null && event.filePhoto != "null" && event.filePhoto != "") {
            var file = new File(this.mainActivity.filesDir, event.filePhoto);
            if (file.exists()) {
                holder.pic.setVisibility(View.VISIBLE);
                holder.pic.setImageURI(Uri.fromFile(file));
            } else {
                holder.pic.setVisibility(View.INVISIBLE);
            }
        } else if (event.fileAudio != null && event.fileAudio != "null" && event.fileAudio != "") {
            holder.pic.setVisibility(View.VISIBLE);
            holder.pic.setImageResource(R.drawable.audio);
        } else {
            holder.pic.setVisibility(View.INVISIBLE);
        }


    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView fullName;
        public final TextView date;
        public final ImageView pic;
        public final ImageView icon;

        public int id;


        public ViewHolder(View view) {
            super(view);
            pic = view.findViewById(R.id.pic);
            icon = view.findViewById(R.id.icon);
            date = view.findViewById(R.id.date);
            fullName = view.findViewById(R.id.full_name_tv);
        }
    }

    class FetchEventsUpdateFromDB extends AsyncTask<String, List<Event>, Boolean> {
        private EventDao dao;
        private Date lastUpdated = null;

        @Override
        protected void onPreExecute() {
            Log.d("FetchEventsUpdateFromDB", "onPreExecute start");
            dao = mainActivity.db.eventDao();
        }

        @Override
        protected Boolean doInBackground(String... args) {
            Log.d("FetchEventsUpdateFromDB", "doInBackground start");
            while (true) {
                try {
                    Log.d("FetchEventsUpdateFromDB", "doInBackground while");
                    var lastUpdated = dao.getMaxUpdated();
                    if (this.lastUpdated == null || this.lastUpdated != lastUpdated) {
                        this.lastUpdated = lastUpdated;
                        Log.d("db", "load data...");
                        var events = dao.getAll(20);
                        super.publishProgress(events);
                    }
                    Thread.sleep(5000);
                } catch (Exception e) {
                    Log.e("network", "FetchData error", e);
                }
            }
        }

        @Override
        protected void onProgressUpdate(List<Event>... value) {
            eventList = value[0];
            Log.d("db", "db count:" + eventList.size());
            notifyDataSetChanged();
        }
    }

}