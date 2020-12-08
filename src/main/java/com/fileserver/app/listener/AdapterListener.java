package com.fileserver.app.listener;

public interface AdapterListener<T> {
    void consumer(T t);
}
