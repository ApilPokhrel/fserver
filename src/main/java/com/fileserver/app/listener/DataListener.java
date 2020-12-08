package com.fileserver.app.listener;

public interface DataListener<T> {
    void onData(T t);
}
