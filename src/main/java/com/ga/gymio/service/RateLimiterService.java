package com.ga.gymio.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class RateLimiterService {

    private final Map<String, RequestInfo> requests = new HashMap<>();

    private static final int MAX_REQUESTS = 5;
    private static final int WINDOW_MINUTES = 1;

    public boolean isAllowed(String ipAddress) {

        LocalDateTime now = LocalDateTime.now();

        RequestInfo requestInfo = requests.get(ipAddress);

        if (requestInfo == null) {
            requests.put(ipAddress, new RequestInfo(now, 1));
            return true;
        }

        if (requestInfo.time.plusMinutes(WINDOW_MINUTES).isBefore(now)) {
            requests.put(ipAddress, new RequestInfo(now, 1));
            return true;
        }

        if (requestInfo.count >= MAX_REQUESTS) {
            return false;
        }

        requestInfo.count++;
        return true;
    }

    private static class RequestInfo {
        private LocalDateTime time;
        private int count;

        public RequestInfo(LocalDateTime time, int count) {
            this.time = time;
            this.count = count;
        }
    }
}