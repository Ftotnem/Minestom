// DataLoadingComponent.java
package nub.wi1helm.server;

import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;

public abstract class DataLoadingComponent<T> {
    private final String name;
    private final boolean required;
    private CompletableFuture<T> loadingFuture;
    private T data;
    private boolean loaded = false;
    private Consumer<T> onLoadCallback;

    public DataLoadingComponent(String name, boolean required) {
        this.name = name;
        this.required = required;
    }

    public abstract CompletableFuture<T> loadData(String uuid, String username);

    public CompletableFuture<T> startLoading(String uuid, String username) {
        if (loadingFuture != null) {
            return loadingFuture;
        }

        loadingFuture = loadData(uuid, username)
                .thenApply(result -> {
                    this.data = result;
                    this.loaded = true;
                    if (onLoadCallback != null) {
                        onLoadCallback.accept(result);
                    }
                    return result;
                });

        return loadingFuture;
    }

    public void setOnLoadCallback(Consumer<T> callback) {
        this.onLoadCallback = callback;
        if (loaded && callback != null) {
            callback.accept(data);
        }
    }

    public String getName() { return name; }
    public boolean isRequired() { return required; }
    public boolean isLoaded() { return loaded; }
    public T getData() { return data; }
    public CompletableFuture<T> getFuture() { return loadingFuture; }
}