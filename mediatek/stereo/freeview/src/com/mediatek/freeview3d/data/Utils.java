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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.mediatek.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/*
 * Common utils for FreeView.
 */
public class Utils {
    private static final String TAG = Log.Tag("Fv/Utils");

    public static final int INSAMPLESIZE = 2;

    public static Bitmap decodeBitmap(Context context, Uri uri, BitmapFactory.Options options) {
        Bitmap bitmap = null;
        ParcelFileDescriptor fd = null;
        try {
            fd = context.getContentResolver().openFileDescriptor(uri, "r");
            setOptionsMutable(options);
            bitmap = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            closeSilently(fd);
        }
        return bitmap;
    }

    /**
     * Decode bitmap by file path.
     * @param filePath to be decoded file path.
     * @return return the decoded bitmap.
     */
    public static Bitmap decodeBitmap(String filePath) {
        Log.d(TAG, "<decodeBitmap> filePath: " + filePath);
        if (filePath == null) {
            Log.d(TAG, "<decodeBitmap> bad argument to decode bitmap");
            return null;
        }

        Bitmap bitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = INSAMPLESIZE;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inMutable = true;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            FileDescriptor fd = fis.getFD();
            bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
        } catch (FileNotFoundException ex) {
            Log.w(TAG, "<decodeBitmap> FileNotFoundException: " + ex);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException t) {
                    Log.w(TAG, "<decodeBitmap> IOException: ", t);
                }
            }
        }
        return bitmap;
    }

    public static void setOptionsMutable(Options options) {
        options.inMutable = true;
    }

    public static void closeSilently(ParcelFileDescriptor fd) {
        try {
            if (fd != null) {
                fd.close();
            }
        } catch (IOException e) {
            Log.w(TAG, "<closeSilently> fail to close ParcelFileDescriptor", e);
        }
    }
}
