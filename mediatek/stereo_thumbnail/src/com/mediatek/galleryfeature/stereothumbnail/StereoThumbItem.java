
package com.mediatek.galleryfeature.stereothumbnail;

import com.mediatek.gallerybasic.base.ExtItem;
import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.ThumbType;

import java.util.ArrayList;

class StereoThumbItem extends ExtItem {
    public StereoThumbItem(MediaData md) {
        super(md);
    }

    @Override
    public boolean isNeedToCacheThumb(ThumbType thumbType) {
        return false;
    }

    @Override
    public boolean isNeedToGetThumbFromCache(ThumbType thumbType) {
        return false;
    }

    @Override
    public ArrayList<SupportOperation> getNotSupportedOperations() {
        ArrayList<SupportOperation> res = new ArrayList<SupportOperation>();
        res.add(SupportOperation.FULL_IMAGE);
        res.add(SupportOperation.EDIT);
        res.add(SupportOperation.CROP);
        res.add(SupportOperation.ROTATE);
        res.add(SupportOperation.SETAS);
        res.add(SupportOperation.SHARE);
        res.add(SupportOperation.PRINT);
        res.add(SupportOperation.INFO);
        return res;
    }

    @Override
    public boolean supportHighQuality() {
        return false;
    }
}