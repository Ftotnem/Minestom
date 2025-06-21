// PlayerDataManager.java
package nub.wi1helm.player;

import nub.wi1helm.data.ProfileDataComponent;
import nub.wi1helm.server.DataLoadingComponent;
import nub.wi1helm.server.ServerProfile;

import static nub.wi1helm.Main.logger;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class PlayerDataManager {
    private final Map<String, DataLoadingComponent<?>> components = new HashMap<>();
    private final List<String> requiredComponents = new ArrayList<>();

    public PlayerDataManager() {
        registerComponent(new ProfileDataComponent());
        }

    public void registerComponent(DataLoadingComponent<?> component) {
        components.put(component.getName(), component);
        if (component.isRequired()) {
            requiredComponents.add(component.getName());
        }
    }

    public CompletableFuture<Void> loadRequiredData(String uuid, String username) {
        List<CompletableFuture<?>> requiredFutures = new ArrayList<>();

        for (String componentName : requiredComponents) {
            DataLoadingComponent<?> component = components.get(componentName);
            if (component != null) {
                requiredFutures.add(component.startLoading(uuid, username));
            }
        }

        return CompletableFuture.allOf(requiredFutures.toArray(new CompletableFuture[0]))
                .thenRun(() -> logger.info("All required data loaded for {}", username));
    }

    public void loadOptionalData(String uuid, String username) {
        components.values().stream()
                .filter(component -> !component.isRequired())
                .forEach(component -> {
                    component.startLoading(uuid, username)
                            .thenRun(() -> logger.info("Optional data {} loaded for {}",
                                    component.getName(), username))
                            .exceptionally(ex -> {
                                logger.warn("Failed to load optional data {} for {}: {}",
                                        component.getName(), username, ex.getMessage());
                                return null;
                            });
                });
    }

    @SuppressWarnings("unchecked")
    public <T> T getComponentData(String componentName, Class<T> type) {
        DataLoadingComponent<?> component = components.get(componentName);
        if (component != null && component.isLoaded()) {
            return (T) component.getData();
        }
        return null;
    }

    public boolean isComponentLoaded(String componentName) {
        DataLoadingComponent<?> component = components.get(componentName);
        return component != null && component.isLoaded();
    }

    public <T> void setComponentCallback(String componentName, java.util.function.Consumer<T> callback) {
        @SuppressWarnings("unchecked")
        DataLoadingComponent<T> component = (DataLoadingComponent<T>) components.get(componentName);
        if (component != null) {
            component.setOnLoadCallback(callback);
        }
    }

    public ServerProfile getProfile() {
        return getComponentData("Profile", ServerProfile.class);
    }

}