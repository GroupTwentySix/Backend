package cc.grouptwentysix.vitality.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RateLimiter {
    private final int maxRequests;
    private final long timeWindowMs;

    //stores timestamps
    private final ConcurrentHashMap<String, RequestData> requestMap = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, long timeWindowMs) {
        this.maxRequests = maxRequests;
        this.timeWindowMs = timeWindowMs;
    }

    public boolean isAllowed(String key) {
        long now = System.currentTimeMillis();

        RequestData data = requestMap.computeIfAbsent(key, k -> new RequestData(0, now));

        synchronized (data) {
            //checks if the time window has passed
            if (now - data.timestamp > timeWindowMs) {
                // Reset the counter and timestamp
                data.requestCount = 1;
                data.timestamp = now;
                return true;
            }

            //checks the request count if it's still in the window
            if (data.requestCount < maxRequests) {
                data.requestCount++;
                return true;
            } else {
                return false;
            }
        }
    }

    private static class RequestData {
        private int requestCount;
        private long timestamp;

        public RequestData(int requestCount, long timestamp) {
            this.requestCount = requestCount;
            this.timestamp = timestamp;
        }
    }


    public static RateLimiter perSecond(int maxRequests) {
        return new RateLimiter(maxRequests, TimeUnit.SECONDS.toMillis(1));
    }

    public static RateLimiter perMinute(int maxRequests) {
        return new RateLimiter(maxRequests, TimeUnit.MINUTES.toMillis(1));
    }
}
