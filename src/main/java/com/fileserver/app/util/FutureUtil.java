package com.fileserver.app.util;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FutureUtil {

    private FutureUtil() {
    }

    public static <T> CompletableFuture<List<T>> allOf(List<CompletableFuture<T>> futuresList) {
        CompletableFuture<Void> allFuturesResult = CompletableFuture
                .allOf(futuresList.toArray(new CompletableFuture[futuresList.size()]));
        return allFuturesResult
                .thenApply(v -> futuresList.stream().map(CompletableFuture::join).collect(Collectors.<T>toList()));
    }
}
