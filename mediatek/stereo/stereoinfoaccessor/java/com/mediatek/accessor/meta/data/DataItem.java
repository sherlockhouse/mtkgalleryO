package com.mediatek.accessor.meta.data;

import java.util.ArrayList;

/**
 * Data item definition.
 */
public class DataItem {
    public static final int DEST_TYPE_STANDARD_XMP = 0;
    public static final int DEST_TYPE_EXTENDED_XMP = 1;

    /**
     * ArrayItem definition.
     */
    public static class ArrayItem {
        public int dest;
        public NameSpaceItem nameSpaceItem;
        public String arrayName;
        public int index;
        public String value;
    }

    /**
     * BufferItem definition.
     */
    public static class BufferItem {
        public int dest;
        public NameSpaceItem nameSpaceItem;
        public String name;
        public byte[] value;
    }

    /**
     * NameSpaceItem definition.
     */
    public static class NameSpaceItem {
        public int dest;
        public String nameSpace;
        public String nameSpacePrifix;
        @Override
        public String toString() {
            return nameSpace + ":" + nameSpacePrifix;
        }
    }

    /**
     * SimpleItem definition.
     */
    public static class SimpleItem {
        public int dest;
        public NameSpaceItem nameSpaceItem;
        public String name;
        public String value;
    }

    /**
     * StructItem definition.
     */
    public static class StructItem {
        public int dest;
        public NameSpaceItem structNameSpaceItem;
        public NameSpaceItem fieldNameSpaceItem;
        public String structName;
        public String fieldName;
        public String fieldValue;
        @Override
        public String toString() {
            return structNameSpaceItem + "|" + fieldNameSpaceItem
                    + ", structName: " + structName + ", fieldName: "
                    + fieldName + ", fieldValue: " + fieldValue;
        }
    }

    /**
     * DataCollections definition.
     */
    public static class DataCollections {
        public int dest;
        public ArrayList<SimpleItem> listOfSimpleValue;
        public ArrayList<BufferItem> listOfBufferItem;
        public ArrayList<ArrayItem> listOfArrayItem;
        public ArrayList<StructItem> listOfStructItem;
        public ArrayList<BufferItem> listOfCustomDataItem;
    }

    /**
     * Rect.
     */
    public static class Rect {
        public int left;
        public int top;
        public int right;
        public int bottom;

        /**
         * Construct Rect.
         * @param left left
         * @param top top
         * @param right right
         * @param bottom bottom
         */
        public Rect(int left, int top, int right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
