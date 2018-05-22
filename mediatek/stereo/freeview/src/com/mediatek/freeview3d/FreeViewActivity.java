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

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ProgressBar;

import com.mediatek.freeview3d.Presentation.PresentationListener;
import com.mediatek.util.Log;

import java.io.File;

/**
 * Control life cycle in FreeView.
 */
public class FreeViewActivity extends Activity implements PresentationListener {
    private static final String TAG = Log.Tag("Fv/FreeViewActivity");

    public static final String KEY_FILE_PATH = "filePath";
    public static final String KEY_SRC_BMP_WIDTH = "srcBmpWidth";
    public static final String KEY_SRC_BMP_HEIGHT = "srcBmpHeight";
    public static final String FREEVIEW_ACTION = "com.android.gallery3d.action.FREEVIEW";
    public static final boolean DEBUG_FREEVIEW = (new File(Environment
            .getExternalStorageDirectory(), "DEBUG_FREEVIEW")).exists();
    private static final int MSG_SHOW_BAR = 1;
    private static final int MSG_UPDATE_DEBUGVIEW = 2;
    private static final int MSG_HIDE_BAR = 3;

    private ActionBar mActionBar;
    private Presentation mFreeViewPresentation;
    private ProgressBar mProgressBar;
    private Handler mHandler;
    private String mFileName;
    private Renderer mRenderer;
    private DebugViewController mDebugViewController;
    private boolean mShowDebugView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Log.d(TAG, " <onCreate> inlet");
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        mActionBar = this.getActionBar();

        setContentView(R.layout.m_freeview);
        mRenderer = (Renderer) findViewById(R.id.gl_freeview_view);
        mProgressBar = (ProgressBar) findViewById(R.id.loading);
        //SynchronizedHandler mSynHandler = new SynchronizedHandler(mRenderer);
        mFreeViewPresentation = new Presentation(this, this, new SynchronizedHandler(mRenderer));
        mRenderer.setListener(mFreeViewPresentation);
        mFileName = getFileName();
        Log.d(TAG, " <onCreate> mFreeViewPresentation & mFileName: " +
                mFreeViewPresentation + " " + mFileName);

        mHandler = new Handler(getMainLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_SHOW_BAR:
                        mProgressBar.setVisibility(View.VISIBLE);
                        break;
                    case MSG_HIDE_BAR: {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        break;
                    }
                    case MSG_UPDATE_DEBUGVIEW: {
                        if (mShowDebugView && mDebugViewController != null) {
                            mDebugViewController.updateView();
                        }
                        break;
                    }
                }
            }
        };
        Log.d(TAG, " <onCreate> DEBUG_FREEVIEW: " + DEBUG_FREEVIEW);
        if (DEBUG_FREEVIEW) {
            mDebugViewController =
                    new DebugViewController(this, (ViewGroup) findViewById(R.id.freeview_root));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        loadMenu(menu);
        int options = 0;
        options |= ActionBar.DISPLAY_HOME_AS_UP;
        mActionBar.setDisplayOptions(options, ActionBar.DISPLAY_HOME_AS_UP);
        mActionBar.setHomeButtonEnabled(true);
        mActionBar.setTitle(mFileName);
        mActionBar.setDisplayShowHomeEnabled(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int action = item.getItemId();
        switch (action) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.m_show:
                mDebugViewController.show();
                mShowDebugView = true;
                return true;
            case R.id.m_hide:
                mDebugViewController.hide();
                mShowDebugView = false;
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, " <onPause> inlet");
        mFreeViewPresentation.pause();
        hideProgressBar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, " <onResume> inlet");
        mFreeViewPresentation.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, " <onDestroy> inlet");
        mFreeViewPresentation.destroy();
        if (mDebugViewController != null) {
            mDebugViewController.destroy();
        }
    }

    @Override
    public void onDynamicState(boolean onDynamicState) {
        if (onDynamicState) {
            mHandler.sendEmptyMessage(MSG_HIDE_BAR);
        } else {
            mHandler.sendEmptyMessage(MSG_SHOW_BAR);
        }
    }

    @Override
    public void doPresentation() {
        if (mShowDebugView && mDebugViewController != null) {
            mHandler.sendEmptyMessage(MSG_UPDATE_DEBUGVIEW);
        }
    }

    private void loadMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.m_freeview_actionbar, menu);
        if (DEBUG_FREEVIEW) {
            MenuItem item = menu.findItem(R.id.m_show);
            item.setVisible(true);
            item = menu.findItem(R.id.m_hide);
            item.setVisible(true);
        } else {
            MenuItem item = menu.findItem(R.id.m_show);
            item.setVisible(false);
            item = menu.findItem(R.id.m_hide);
            item.setVisible(false);
        }
    }

    private void hideProgressBar() {
        mProgressBar.setVisibility(View.INVISIBLE);
    }

    private String getFileName() {
        String filePath = null;
        Intent intent = getIntent();
        if (intent != null) {
            filePath = intent.getExtras().getString(FreeViewActivity.KEY_FILE_PATH);
        } else {
            Log.d(TAG, " <getFileName> intent is empty, do nothing.");
            return null;
        }

        if (filePath == null || filePath.equals("")) {
            Log.d(TAG, " <getFileName> file path is null, cannot get file name.");
            return null;
        }
        String[] path = filePath.split("/");
        if (path.length == 0) {
            Log.d(TAG, " <getFileName> path is empty, do nothing.");
            return null;
        }
        Log.d(TAG, "<getFileName> filePath & path: " + filePath + " " + path[path.length - 1]);
        String[] name = path[path.length - 1].split("\\.");
        return name[0];
    }
}
