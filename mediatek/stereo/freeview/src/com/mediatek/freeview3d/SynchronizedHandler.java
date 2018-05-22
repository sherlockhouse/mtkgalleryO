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

import android.os.Handler;
import android.os.Message;

import com.mediatek.util.Log;

/*
 * Customized the synchronization class, locked for the message mainly.
 */
public class SynchronizedHandler extends Handler {
    private static final String TAG = Log.Tag("Fv/SynchronizedHandler");

    private final Renderer mRenderer;

    public SynchronizedHandler(Renderer renderer) {
        mRenderer = renderer;
    }

    @Override
    public void dispatchMessage(Message message) {
        mRenderer.lockRenderThread();
        try {
            super.dispatchMessage(message);
        } finally {
            mRenderer.unlockRenderThread();
        }
    }
}
