package tech.bystep.planificador.api;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitService {

    private final Map<String, Deque<Long>> store = new ConcurrentHashMap<>();

    /**
     * Sliding-window rate limit.
     * @param key        unique key (e.g. "support:192.168.1.1")
     * @param maxRequests max allowed requests inside the window
     * @param windowMs   window size in milliseconds
     * @return true if request is allowed
     */
    public boolean isAllowed(String key, int maxRequests, long windowMs) {
        long now = System.currentTimeMillis();
        Deque<Long> timestamps = store.compute(key, (k, deque) -> {
            if (deque == null) deque = new ArrayDeque<>();
            while (!deque.isEmpty() && now - deque.peekFirst() > windowMs) {
                deque.pollFirst();
            }
            deque.addLast(now);
            return deque;
        });
        return timestamps.size() <= maxRequests;
    }
}
