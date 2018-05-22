package com.mediatek.photopicker.data;

import android.net.Uri;

/**
 * Base class of AlbumItem and FileItem.
 */
public class Item {
    public Uri pathUri;
    public String title;

    // Expandable
    public Object object;
}
