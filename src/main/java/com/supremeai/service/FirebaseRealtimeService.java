package com.supremeai.service;

import com.google.firebase.database.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Service to interact with Firebase Realtime Database.
 * Wraps the blocking Admin SDK calls into reactive Monos.
 */
@Service
public class FirebaseRealtimeService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseRealtimeService.class);

    /**
     * Fetch data from a specific path in Realtime Database.
     * 
     * @param path The database path (e.g., "config/api_keys")
     * @return Data as a Map, wrapped in Mono
     */
    public Mono<Map<String, Object>> getData(String path) {
        return Mono.create(sink -> {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference(path);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                @SuppressWarnings("unchecked")
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Object value = snapshot.getValue();
                        if (value instanceof Map) {
                            sink.success((Map<String, Object>) value);
                        } else {
                            log.warn("Data at path {} is not a Map: {}", path, value);
                            sink.success();
                        }
                    } else {
                        log.debug("No data found at path: {}", path);
                        sink.success();
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    log.error("Firebase RTDB error at path {}: {}", path, error.getMessage());
                    sink.error(new RuntimeException("Firebase RTDB error: " + error.getMessage()));
                }
            });
        });
    }

    /**
     * Save data to a specific path in Realtime Database.
     */
    public Mono<Void> setData(String path, Object value) {
        return Mono.create(sink -> {
            FirebaseDatabase.getInstance().getReference(path).setValue(value, (error, ref) -> {
                if (error != null) {
                    sink.error(new RuntimeException("Firebase RTDB write error: " + error.getMessage()));
                } else {
                    sink.success();
                }
            });
        });
    }

    /**
     * Save data to a specific path in Realtime Database.
     * Alias for setData for compatibility.
     */
    public Mono<Void> saveData(String path, Map<String, Object> data) {
        return setData(path, data);
    }
}
