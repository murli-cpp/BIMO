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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by paetztm on 2/6/2017.
 */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {
    private final LayoutInflater layoutInflater;
    private FetchEventsUpdateFromDB fetchEventsUpdateFromDBTask;
    private List<Event> eventList;
    private final int rowLayout;
    private final MainActivity mainActivity;

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
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Event event = eventList.get(position);
        holder.fullName.setText(event.title);
        holder.pic.setImageURI(Uri.fromFile(new File("storage/180A-251C/Download/cat.jpg")));
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView fullName;
        public final ImageView pic;

        public ViewHolder(View view) {
            super(view);
            pic = view.findViewById(R.id.pic);
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
                        var events = dao.getAll(10);
                        super.publishProgress(events);
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    Log.e("network", "FetchData error", e);
                }
            }
        }

        @Override
        protected void onProgressUpdate(List<Event>... value) {
            eventList = value[0];
            Log.d("db", "db count:"+eventList.size());
            notifyDataSetChanged();
        }
    }

}