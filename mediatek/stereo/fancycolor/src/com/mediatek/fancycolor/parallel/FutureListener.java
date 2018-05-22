package com.mediatek.fancycolor.parallel;

public interface FutureListener<T> {
    public void onFutureDone(Future<T> future);
}
