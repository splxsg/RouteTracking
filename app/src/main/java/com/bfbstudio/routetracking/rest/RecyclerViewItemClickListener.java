package com.bfbstudio.routetracking.rest;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by Blues on 30/03/2017.
 * This listener called back when recycler view item is clicked.
 */

public class RecyclerViewItemClickListener implements RecyclerView.OnItemTouchListener {

    @Override public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    private OnItemClickListener listener;
    private GestureDetector gestureDetector;
    public interface OnItemClickListener{
        void onItemClick(View v, int position);
    }

    public RecyclerViewItemClickListener(Context context, OnItemClickListener listener) {
        this.listener = listener;
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });
    }

    @Override public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
        View childView = view.findChildViewUnder(e.getX(), e.getY());
        /**
         * The comment function can be developed next update to do some gesture detector on recycle view like delete journey history.
         */
        if (childView != null && listener != null && gestureDetector.onTouchEvent(e)) {
        //if (childView != null && listener != null){
            listener.onItemClick(childView, view.getChildPosition(childView));
            return true;
        }
        return false;
    }

    @Override public void onTouchEvent(RecyclerView view, MotionEvent motionEvent) { }
}

