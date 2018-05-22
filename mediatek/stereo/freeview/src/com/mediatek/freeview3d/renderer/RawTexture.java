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

import com.mediatek.util.Log;

import javax.microedition.khronos.opengles.GL11;

/**
 * RawTexture is a Texture corresponds to a real GL texture.
 * The state of a RawTexture indicates whether its data is loaded to GL memory.
 * If a RawTexture is loaded into GL memory, it has a GL texture id.
 */
public class RawTexture extends BasicTexture {
    private static final String TAG = Log.Tag("Fv/RawTexture");

    public RawTexture(int width, int height) {
        setSize(width, height);
    }

    public void prepare(GLES20Canvas canvas) {
        mId = canvas.generateTexture();
        canvas.initializeTextureSize(this, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE);
        canvas.setTextureParameters(this);
        mState = STATE_LOADED;
        setAssociatedCanvas(canvas);
    }

    @Override
    protected boolean onBind(GLES20Canvas canvas) {
        if (isLoaded()) return true;
        Log.w(TAG, "lost the content due to context change");
        return false;
    }

    @Override
     public void yield() {
         // we cannot free the texture because we have no backup.
     }

    @Override
    public int getTarget() {
        return GL11.GL_TEXTURE_2D;
    }
}
