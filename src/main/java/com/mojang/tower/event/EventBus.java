package com.mojang.tower.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Lightweight synchronous publish/subscribe event bus.
 *
 * Events are dispatched synchronously within the same call stack,
 * which is critical for game determinism.
 */
public final class EventBus {
    private static final EventBus INSTANCE = new EventBus();
    private final Map<Class<?>, List<Consumer<?>>> listeners = new ConcurrentHashMap<>();

    private EventBus() {
        // Singleton
    }

    /**
     * Subscribe a listener for events of the given type.
     *
     * @param eventType the class of events to listen for
     * @param listener the consumer to invoke when events are published
     * @param <T> the event type
     */
    public static <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        INSTANCE.listeners
            .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
            .add(listener);
    }

    /**
     * Unsubscribe a listener from events of the given type.
     *
     * @param eventType the class of events to stop listening for
     * @param listener the consumer to remove
     * @param <T> the event type
     */
    public static <T> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        var list = INSTANCE.listeners.get(eventType);
        if (list != null) {
            list.remove(listener);
        }
    }

    /**
     * Publish an event to all registered listeners.
     * Dispatches synchronously - all listeners are invoked before this method returns.
     *
     * @param event the event to publish
     * @param <T> the event type
     */
    @SuppressWarnings("unchecked")
    public static <T> void publish(T event) {
        var list = INSTANCE.listeners.get(event.getClass());
        if (list != null) {
            for (var listener : list) {
                ((Consumer<T>) listener).accept(event);
            }
        }
    }

    /**
     * Clear all registered listeners.
     * Primarily for testing to ensure clean state between tests.
     */
    public static void reset() {
        INSTANCE.listeners.clear();
    }
}
