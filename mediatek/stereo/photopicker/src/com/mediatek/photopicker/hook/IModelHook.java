package com.mediatek.photopicker.hook;

import com.mediatek.photopicker.data.Item;

import java.util.List;

/**
 * IModelHook.
 */
public interface IModelHook {

    /**
     * Get all count by conditions.
     * @return Count.
     */
    public int getCount();

    /**
     * Get items by conditions.
     * @param queryStart null<br>
     * @param queryEnd null<br>
     * @return The return item list.
     */
    public List<Item> getItems(int queryStart, int queryEnd);
}