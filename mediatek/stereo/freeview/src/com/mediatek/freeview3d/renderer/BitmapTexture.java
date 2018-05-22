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

package com.mediatek.freeview3d.renderer;

import android.graphics.Bitmap;

import com.mediatek.util.Log;

import junit.framework.Assert;
import javax.microedition.khronos.opengles.GL11;

/*
 * BitmapTexture is a texture whose content is specified by a fixed Bitmap.
 *
 * 1. By default the texture does not own the Bitmap. The user should make sure the Bitmapis valid
 *    during the texture's lifetime. When the texture is recycled, it does not free the Bitmap.
 * 2. If mNeedRecycleBitmap is true, Recycle the bitmap after uploaded texture for saving memory.
 */
public class BitmapTexture extends BasicTexture {
    private static final String TAG = Log.Tag("Fv/BitmapTexture");

    private boolean mContentValid = true;
    protected Bitmap mBitmap;

    public BitmapTexture(Bitmap bitmap) {
        super(null, 0, STATE_UNLOADED);
        mBitmap = bitmap;
        int w = 0;
        int h = 0;
        if (mBitmap != null) {
            w = mBitmap.getWidth();
            h = mBitmap.getHeight();
            Log.d(TAG, "<constructor> mBitmap'width & height: " + w + " " + h);
        }
        setSize(w, h);
    }

    private void freeBitmap() {
        onFreeBitmap(mBitmap);
        mBitmap = null;
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    protected void onFreeBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
    }

    /**
     * Whether the content on GPU is valid.
     */
    public boolean isContentValid() {
        return isLoaded() && mContentValid;
    }

    /**
     * Updates the content on GPU's memory.
     * @param canvas
     */
    public void updateContent(GLES20Canvas canvas) {
        if (!isLoaded()) {
            uploadToCanvas(canvas);
        }
    }

    private void uploadToCanvas(GLES20Canvas canvas) {
        if (mBitmap != null) {
            try {
                mId = canvas.generateTexture();
                canvas.setTextureParameters(this);
                canvas.initializeTexture(this, mBitmap);
            } finally {
                freeBitmap();
            }
            // Update texture state.
            setAssociatedCanvas(canvas);
            mState = STATE_LOADED;
            mContentValid = true;
        } else {
            mState = STATE_ERROR;
            Log.d(TAG, "<uploadToCanvas> Texture load fail, no bitmap");
        }
    }

    @Override
    protected boolean onBind(GLES20Canvas canvas) {
        updateContent(canvas);
        return isContentValid();
    }

    @Override
    protected int getTarget() {
        return GL11.GL_TEXTURE_2D;
    }

    @Override
    public void recycle() {
        super.recycle();
        synchronized (this) {
            if (mBitmap != null) {
                freeBitmap();
            }
        }
    }
}
