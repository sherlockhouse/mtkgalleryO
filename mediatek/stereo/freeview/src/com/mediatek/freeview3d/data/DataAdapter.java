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

package com.mediatek.freeview3d.data;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import com.mediatek.freeview3d.FreeView3D;
import com.mediatek.freeview3d.FreeViewActivity;
import com.mediatek.freeview3d.SynchronizedHandler;
import com.mediatek.freeview3d.renderer.BasicTexture;
import com.mediatek.freeview3d.renderer.BitmapTexture;
import com.mediatek.freeview3d.renderer.RawTexture;
import com.mediatek.util.Log;
import com.mediatek.util.StereoImage;

/*
 * Produce & manage output textures, and start decoder thread and depthGenerate thread.
 */
public class DataAdapter {
    private static final String TAG = Log.Tag("Fv/DataAdapter");

    private Activity mContext;
    private String mSourcePath;
    private Uri mSourceUri;
    private Bitmap mBitmap;
    private BitmapTexture mTexture = null;
    private DecoderRunnable mDecodRunnable;
    private DepthGenerateRunnable mDepthGenerateRunnable;
    private Thread mDecodeThread;
    private FreeView3D mFreeView3DJni = null;
    private boolean mIsGenerateDepth = false;
    private boolean mIsInitFreeView = false;
    private RawTexture mTextureOutPut = null;
    private DataListener mDataListener;
    private Bitmap mBitmapForDepth = null;
    private SynchronizedHandler mSynchronizedHandler;

    public interface DataListener {
        public void startDecodeBitmap();
        public void hasDecodedBitmap();
        public void hasGeneratedDepth();
    }

    public DataAdapter(Activity context, SynchronizedHandler syncHandler) {
        mContext = context;
        mSynchronizedHandler = syncHandler;
        Intent intent = context.getIntent();
        if (intent != null) {
            mSourcePath = intent.getExtras().getString(FreeViewActivity.KEY_FILE_PATH);
            mSourceUri = intent.getData();
            Log.d(TAG, "<DataAdapter> (constructor)intent & mSourcePath & mSourceUri: " +
                    intent + " " + mSourcePath + " " + mSourceUri);
        } else {
            Log.d(TAG, "<DataAdapter> (constructor)intent is null, do nothing.");
        }
        mSynchronizedHandler.post(new Runnable() {
            @Override
            public void run() {
                mFreeView3DJni = new FreeView3D();
            }
        });
    }

    public boolean hasinitFV() {
        if (mIsGenerateDepth && !mIsInitFreeView) {
            mIsInitFreeView = mFreeView3DJni.initialize(mSourcePath, mBitmapForDepth);
        }
        return mIsInitFreeView;
    }

    public void setDataListener(DataListener listener) {
        mDataListener = listener;
    }

    public void resume() {
        Log.d(TAG, "<resume> inlet (start decoder thread)");
        if (mDecodeThread == null) {
            mDataListener.startDecodeBitmap();
            mDecodRunnable = new DecoderRunnable();
            mDecodeThread = new Thread(mDecodRunnable);
            mDecodeThread.start();
        }
    }

    public void destroy() {
        Log.d(TAG, "<destroy> inlet (stop decoder thread & DepthGenerate thread)");
        if (mDecodRunnable != null) {
            mDecodRunnable.mIsCancelled = true;
            mDecodRunnable = null;
        }
        if (mDepthGenerateRunnable != null) {
            mDepthGenerateRunnable.mIsCancelled = true;
            mDepthGenerateRunnable = null;
        }
        mDecodeThread = null;

        mSynchronizedHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mFreeView3DJni != null) {
                    mFreeView3DJni.release();
                    mFreeView3DJni = null;
                }
                if (mTexture != null) {
                    mTexture.recycle();
                    mTexture = null;
                }
                if (mTextureOutPut != null) {
                    mTextureOutPut.recycle();
                    mTextureOutPut = null;
                }
                mIsInitFreeView = false;
                mIsGenerateDepth = false;
            }
        });
    }

    public BasicTexture getOriginalTexture() {
        return mTexture;
    }

    public BasicTexture getOutputTexture() {
        return mTextureOutPut;
    }

    public boolean process(int outputTextureId, int x, int y) {
        if (mFreeView3DJni != null) {
            return mFreeView3DJni.process(x, y, outputTextureId);
        }
        return false;
    }

    /*
     * Generate depth info.
     */
    private class DepthGenerateRunnable implements Runnable {
        private volatile boolean mIsCancelled;

        @Override
        public void run() {
            if (mFreeView3DJni != null) {
                final boolean hasGenerateDepth =
                        mFreeView3DJni.generateDepthInfo(mContext, mSourceUri, mSourcePath);
                if (mIsCancelled) {
                    return;
                }
                mSynchronizedHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mIsGenerateDepth = hasGenerateDepth;
                        if (mIsGenerateDepth) {
                            mTextureOutPut =
                                    new RawTexture(mBitmap.getWidth(), mBitmap.getHeight());
                            if (mDataListener != null) {
                                mDataListener.hasGeneratedDepth();
                            }
                        }
                    }
                });
            }
        }
    }

    /*
     * Decode stereo image[sample size = 2].
     */
    private class DecoderRunnable implements Runnable {
        private volatile boolean mIsCancelled;

        @Override
        public void run() {
            try {
                mBitmap = StereoImage.decodeBitmap(mContext, mSourcePath, Utils.INSAMPLESIZE);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "IllegalArgumentException: ", e);
            }
            if (mBitmap == null) {
                Log.d(TAG, "<DataAdapter> decode image fail!!");
                return;
            }
            mBitmapForDepth = Bitmap.createBitmap(mBitmap);
            Log.d(TAG, "<DataAdapter> mBitmap'width & height: " +
                    mBitmap.getWidth() + " " + mBitmap.getHeight());
            if (mIsCancelled) {
                return;
            }
            mSynchronizedHandler.post(new Runnable() {
                @Override
                public void run() {
                    mTexture = new BitmapTexture(mBitmap);
                    mDataListener.hasDecodedBitmap();
                    mDepthGenerateRunnable = new DepthGenerateRunnable();
                    Thread thread = new Thread(mDepthGenerateRunnable);
                    thread.start();
                }
            });
        }
    }
}
