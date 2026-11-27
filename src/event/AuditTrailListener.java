package event;

import model.Task;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AuditTrailListener implements TaskEventListener {
    private final Map<String, Integer> eventCounts = new LinkedHashMap<>();

    @Override
    public void onEvent(String eventType, Task task) {
        if (eventType == null) return;
        eventCounts.put(eventType, eventCounts.getOrDefault(eventType, 0) + 1);
    }

    public Map<String, Integer> getEventCounts() {
        return Collections.unmodifiableMap(eventCounts);
    }
}