package com.bfbstudio.routetracking.RecycleAdapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bfbstudio.routetracking.R;
import com.bfbstudio.routetracking.data.JourneyContract;
import com.bfbstudio.routetracking.data.JourneyContract.LocationEntry;
import com.bfbstudio.routetracking.data.JourneyContract.JourneyEntry;
import com.bfbstudio.routetracking.rest.CustomSharedPreference;
import com.bfbstudio.routetracking.rest.Utility;

/**
 * Created by Blues on 30/03/2017.
 */

/**
 * This recycler view adapter is defined for adapting bottom sheet that shows journey history
 */
public class JourneyRecycleAdapter extends RecyclerView.Adapter<JourneyRecycleAdapter.JourneyRecycleViewHolder>{

    private final LayoutInflater mLayoutInflater;
    private Cursor mCursor;
    private final Context mContext;


    public JourneyRecycleAdapter(Context context, View emptyview) {
        this.mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mCursor = mContext.getContentResolver().query(
                JourneyContract.JourneyEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );
    }
    @Override
    public JourneyRecycleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new JourneyRecycleViewHolder(mLayoutInflater.inflate(R.layout.adapter, parent, false));
    }
    @Override
    public void onBindViewHolder(JourneyRecycleViewHolder holder, int position){
        mCursor.moveToPosition(position);
        holder.mJourneyDate.setText(mCursor.getString(mCursor.getColumnIndex(JourneyEntry.COLUMN_JOURNEY_DATE)));
        holder.mJourneyDuration.setText(Utility.millisectoSimpleTimeFormat(Long.parseLong((mCursor.getString(mCursor.getColumnIndex(JourneyEntry.COLUMN_JOURNEY_DURATION))))));
        holder.mJourneyDistance.setText(Utility.formatDistance(mCursor.getDouble(mCursor.getColumnIndex(JourneyEntry.COLUMN_JOURNEY_DISTANCE))));
    }

    @Override
    public int getItemCount() {
        if(mCursor == null) return 0;
        return mCursor.getCount();

    }
    public static class JourneyRecycleViewHolder extends RecyclerView.ViewHolder {
        TextView mJourneyDate;
        TextView mJourneyDuration;
        TextView mJourneyDistance;
        JourneyRecycleViewHolder(View view) {
            super(view);
            mJourneyDate = (TextView) view.findViewById(R.id.list_journey_start_time);
            mJourneyDuration = (TextView) view.findViewById(R.id.list_journey_duration);
            mJourneyDistance = (TextView) view.findViewById(R.id.list_journey_distance);
        }
    }
}