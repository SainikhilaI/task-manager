package repository;

import api.ApiResponse;
import java.util.HashMap;
import java.util.Map;

public class IdempotencyStore {
    private static volatile IdempotencyStore instance;

    private final Map<String, StoredRecord> records = new HashMap<>();

    private IdempotencyStore() {
    }

    public static IdempotencyStore getInstance() {
        if (instance == null) {
            synchronized (IdempotencyStore.class) {
                if (instance == null) {
                    instance = new IdempotencyStore();
                }
            }
        }
        return instance;
    }

    public synchronized StoredRecord get(String key) {
        if (key == null) return null;
        return records.get(key);
    }

    public synchronized void putSuccess(String key, String fingerprint, ApiResponse response) {
        if (key == null || response == null) return;
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
            records.put(key, new StoredRecord(fingerprint, response));
        }
    }

    public synchronized void clear() {
        records.clear();
    }

    public static class StoredRecord {
        private final String fingerprint;
        private final ApiResponse response;

        public StoredRecord(String fingerprint, ApiResponse response) {
            this.fingerprint = fingerprint;
            this.response = response;
        }

        public String getFingerprint() { return fingerprint; }
        public ApiResponse getResponse() { return response; }
    }
}