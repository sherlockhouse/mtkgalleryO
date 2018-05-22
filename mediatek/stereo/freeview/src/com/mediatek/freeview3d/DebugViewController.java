/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.freeview3d;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.mediatek.freeview3d.animation.AnimationEx;
import com.mediatek.util.Log;


/*
 * For debug view in FreeView, such as show current FPS proprieties.
 */
public class DebugViewController implements SeekBar.OnSeekBarChangeListener {
    private final static String TAG = "FV/DebugViewController";

    private Context contenxt;
    private LayoutInflater mInflater;
    private View mDebugView;
    private ViewGroup mRootView;
    private SeekBar mSeekbar;
    private final static int sMinStepValue = 10;
    private final static int sMaxStepValue = 100;
    private static int mCurrentStepValue = 30;
    private TextView mCurrentValueView;
    private TextView mFPSView;

    public DebugViewController(Context context, ViewGroup root) {
        mRootView = root;
        mInflater = LayoutInflater.from(context);
        mDebugView = mInflater.inflate(R.layout.m_debug_seekbar, root, false);
        TextView minValueView = (TextView) mDebugView.findViewById(R.id.m_textViewMinValue);
        minValueView.setText("" + sMinStepValue);
        TextView maxValueView = (TextView) mDebugView.findViewById(R.id.m_textViewMaxValue);
        maxValueView.setText("" + sMaxStepValue);
        mCurrentValueView = (TextView) mDebugView.findViewById(R.id.m_textViewCurrentIndex);
        mCurrentValueView.setText("" + mCurrentStepValue);
        mFPSView = (TextView) mDebugView.findViewById(R.id.m_pfs);
        mFPSView.setText("FPS : " + Renderer.FPS);
        mSeekbar = (SeekBar) mDebugView.findViewById(R.id.m_seekbar);
        mSeekbar.setOnSeekBarChangeListener(this);
        mSeekbar.setLeft(sMinStepValue);
        mSeekbar.setMax(100);
        mSeekbar.setProgress(mCurrentStepValue);
        mRootView.addView(mDebugView);
        mDebugView.setVisibility(View.INVISIBLE);
    }

    public void show() {
        if (mDebugView != null) {
            Log.d(TAG, " <show> set seekbar visible");
            mDebugView.setVisibility(View.VISIBLE);
        }
    }

    public void hide() {
        if (mDebugView != null) {
            Log.d(TAG, " <hide> set seekbar invisible");
            mDebugView.setVisibility(View.INVISIBLE);
        }
    }

    public void destroy() {
        if (mRootView != null && mDebugView != null) {
            mRootView.removeView(mDebugView);
        }
    }

    public void updateView() {
        if (mFPSView != null) {
            mFPSView.setText("FPS : " + Renderer.FPS);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            int currentValue = sMinStepValue +
                               (int) ((float) progress * (sMaxStepValue - sMinStepValue)) / 100;
            mCurrentValueView.setText("" + currentValue);
            AnimationEx.STEP = currentValue;
            Log.d(TAG, " <onProgressChanged> currentValuce: " + currentValue);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStopTrackingTouch(SeekBar arg0) {
        // TODO Auto-generated method stub
    }

}
