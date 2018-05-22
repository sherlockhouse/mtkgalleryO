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
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.mediatek.freeview3d.renderer.GLES20Canvas;
import com.mediatek.util.Log;

import java.io.File;
import java.util.concurrent.locks.ReentrantLock;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

/**
 * Implement callback in GLSurfaceView.
 */
public class Renderer extends GLSurfaceView implements GLSurfaceView.Renderer {
    private static final String TAG = Log.Tag("Fv/Renderer");

    private static final int FLAG_INITIALIZED = 1;
    private static final int FLAG_NEED_LAYOUT = 2;
    private GL11 mGL;
    private GLES20Canvas mCanvas;
    private RenderListener mListener;
    private final ReentrantLock mRenderLock = new ReentrantLock();
    private int mFlags = FLAG_NEED_LAYOUT;
    private int mWidth;
    private int mHeight;

    // For debug FPS
    public static double FPS = 0;
    private static final boolean DEBUG_FPS = (new File(Environment
            .getExternalStorageDirectory(), "DEBUG_FREEVIEW")).exists();
    private int mFrameCount = 0;
    private long mFrameCountingStart = 0;

    public interface RenderListener {
        public void render(GLES20Canvas canvas);
        public boolean touchEvent(MotionEvent event);
        public void layout(int left, int top, int right, int bottom);
    }

    public Renderer(Context context) {
        this(context, null);
    }

    public Renderer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mFlags |= FLAG_INITIALIZED;
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 0, 0, 0);
        setRenderer(this);
        getHolder().setFormat(PixelFormat.RGB_888);
    }

    public void setListener(RenderListener content) {
        mListener = content;
        if (content != null) {
            requestLayoutContent();
        }
    }

    @Override
    public void requestRender() {
        super.requestRender();
    }

    public void requestLayoutContent() {
        if (mListener == null || (mFlags & FLAG_NEED_LAYOUT) != 0)
            return;
        // "View" system will invoke onLayout() for initialization(bug ?), we
        // have to ignore it since the GLThread is not ready yet.
        if ((mFlags & FLAG_INITIALIZED) == 0)
            return;

        mFlags |= FLAG_NEED_LAYOUT;
        requestRender();
    }

    private void layoutContentPane() {
        mFlags &= ~FLAG_NEED_LAYOUT;
        int w = getWidth();
        int h = getHeight();
        if (mWidth != w || mHeight != h) {
            mWidth = w;
            mHeight = h;
        }
        if (mListener != null && w != 0 && h != 0) {
            mListener.layout(0, 0, w, h);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            requestLayoutContent();
        }
    }

    // Called when the context is created, possibly after automatic destruction.
    @Override
    public void onSurfaceCreated(GL10 gl1, EGLConfig config) {
        GL11 gl = (GL11) gl1;
        if (mGL != null) {
            // The GL Object has changed
            Log.i(TAG, "GLObject has changed from " + mGL + " to " + gl);
        }
        mGL = gl;
        mCanvas = new GLES20Canvas();
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    // Called when the OpenGL surface is recreated without destroying the context.
    @Override
    public void onSurfaceChanged(GL10 gl1, int width, int height) {
        Log.i(TAG, "onSurfaceChanged: " + width + " " + height);
        GL11 gl = (GL11) gl1;
        assert (mGL == gl);
        mCanvas.setSize(width, height);
        mFlags |= FLAG_NEED_LAYOUT;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mRenderLock.lock();
        try {
            onDrawFrameLocked(gl);
        } finally {
            mRenderLock.unlock();
        }

        if (DEBUG_FPS) {
            long now = System.nanoTime();
            if (mFrameCountingStart == 0) {
                mFrameCountingStart = now;
            } else if ((now - mFrameCountingStart) > 1e+9f) {
                FPS = (double) mFrameCount * 1e+9f / (now - mFrameCountingStart);
                Log.d(TAG, "<onDrawFrame> FPS: " + FPS);
                mFrameCountingStart = now;
                mFrameCount = 0;
            }
            ++mFrameCount;
        }
    }

    public void lockRenderThread() {
        mRenderLock.lock();
    }

    public void unlockRenderThread() {
        mRenderLock.unlock();
    }

    private void onDrawFrameLocked(GL10 gl) {
        // release the unbound textures and deleted buffers.
        mCanvas.deleteRecycledResources();
        if ((mFlags & FLAG_NEED_LAYOUT) != 0) {
            layoutContentPane();
        }
        mCanvas.save(GLES20Canvas.SAVE_FLAG_ALL);
        if (mListener != null) {
            mListener.render(mCanvas);
        }
        mCanvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mListener != null) {
            mListener.touchEvent(event);
        }
        return true;
    }
}
