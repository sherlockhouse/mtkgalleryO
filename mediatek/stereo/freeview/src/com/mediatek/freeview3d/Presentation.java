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

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.mediatek.freeview3d.Renderer.RenderListener;
import com.mediatek.freeview3d.animation.AnimationEx;
import com.mediatek.freeview3d.data.DataAdapter;
import com.mediatek.freeview3d.data.DataAdapter.DataListener;
import com.mediatek.freeview3d.renderer.BasicTexture;
import com.mediatek.freeview3d.renderer.GLES20Canvas;
import com.mediatek.util.Log;

/*
 * Implement interface render(), layout(), and consume texture in renderer.
 */
public class Presentation implements DataListener, RenderListener {
    private final static String TAG = Log.Tag("Fv/Presentation");

    public AnimationEx mAnimation;
    private Activity mActivity;
    private DataAdapter mDataAdapter;
    private boolean mDirty;
    private BasicTexture mCurrentTexture;
    private PresentationListener mPresentationListener;
    private Rect mTextureRect = null;
    private RectF mPanelRect = null;
    private Rect mDisplayRect = null;
    private SynchronizedHandler mSynchronizedHandler;

    public interface PresentationListener {
        public void onDynamicState(boolean onDynamicState);
        public void doPresentation();
    }

    public Presentation(Activity activity, PresentationListener listener,
            SynchronizedHandler handler) {
        mActivity = activity;
        mPresentationListener = listener;
        mSynchronizedHandler = handler;
        mDataAdapter = new DataAdapter(activity, mSynchronizedHandler);
        mDataAdapter.setDataListener(this);

        WindowManager wm = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics reMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(reMetrics);
        mDisplayRect = new Rect(0, 0, reMetrics.widthPixels, reMetrics.heightPixels);
    }

    public void resume() {
        mDirty = true;
        mDataAdapter.resume();
        if (mAnimation != null) {
            mAnimation.startAnimation();
        }
    }

    public void destroy() {
        mDataAdapter.destroy();
    }

    public void pause() {
        mDirty = false;
        if (mAnimation != null) {
            mAnimation.stopAnimation();
        }
    }

    @Override
    public void hasDecodedBitmap() {
        BasicTexture texture = mDataAdapter.getOriginalTexture();
        mTextureRect = new Rect(0, 0, texture.getWidth(), texture.getHeight());
        Log.d(TAG, "<hasDecodedBitmap> texture & mTextureRect: " +
                texture + " " + mTextureRect.toShortString());
        mDirty = true;
    }

    private Rect getDisplayRect() {
        if (mDirty && mPanelRect != null && mTextureRect != null) {
            RectF temp = new RectF(mTextureRect);
            Matrix matrix = new Matrix();
            matrix.setRectToRect(temp, mPanelRect, Matrix.ScaleToFit.CENTER);
            matrix.mapRect(temp);
            int left = (int) temp.left;
            int top = (int) temp.top;
            int right = (int) temp.right;
            int bottom = (int) temp.bottom;
            mDisplayRect = new Rect(left, top, right, bottom);
            mDirty = false;
            Log.d(TAG, "<getDisplayRect> rect: " + mDisplayRect.toString());
        }
        return mDisplayRect;
    }

    private AnimationEx createAnimation() {
        BasicTexture texture = mDataAdapter.getOriginalTexture();
        int textWidth = texture.getWidth();
        int textHeight = texture.getHeight();
        return new AnimationEx(mActivity, mSynchronizedHandler, textWidth, textHeight,
                getDisplayRect());
    }

    @Override
    public void hasGeneratedDepth() {
        mPresentationListener.onDynamicState(true);
    }

    @Override
    public boolean touchEvent(MotionEvent event) {
        if (mAnimation != null) {
            return mAnimation.touchEvent(event);
        }
        return false;
    }

    @Override
    public void render(GLES20Canvas canvas) {
        draw(canvas);
    }

    @Override
    public void layout(int left, int top, int right, int bottom) {
        mPanelRect = new RectF(left, top, right, bottom);
        Log.d(TAG, "<layout> (set panel rect)mPanelRect: " + mPanelRect.toString());
        mDirty = true;
    }

    private void draw(GLES20Canvas canvas) {
        Rect r = getDisplayRect();
        int drawW = r.width();
        int drawH = r.height();
        int cx = r.centerX();
        int cy = r.centerY();
        canvas.save(GLES20Canvas.SAVE_FLAG_MATRIX | GLES20Canvas.SAVE_FLAG_ALPHA);
        canvas.translate(cx, cy);
        if (mDataAdapter.hasinitFV()) {
            if (mAnimation == null) {
                mAnimation = createAnimation();
            }
            boolean isFinished = mAnimation.isFinished();
            mCurrentTexture = mDataAdapter.getOutputTexture();
            if (!mCurrentTexture.isLoaded() || !isFinished) {
                canvas.beginRenderTarget(mCurrentTexture);
                int[] index = mAnimation.getCurrentFrame();
                mDataAdapter.process(mCurrentTexture.getId(), index[0], index[1]);
                canvas.endRenderTarget();
            }
        } else {
            mCurrentTexture = mDataAdapter.getOriginalTexture();
        }
        if (mCurrentTexture != null) {
            mCurrentTexture.draw(canvas, -drawW / 2, -drawH / 2, drawW, drawH);
        }
        if (mPresentationListener != null) {
            mPresentationListener.doPresentation();
        }
        canvas.restore();
    }

    @Override
    public void startDecodeBitmap() {
        mPresentationListener.onDynamicState(false);
    }
}
