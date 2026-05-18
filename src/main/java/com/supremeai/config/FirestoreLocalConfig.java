package com.supremeai.config;

import com.google.cloud.spring.data.firestore.FirestoreTemplate;
import com.google.cloud.spring.data.firestore.mapping.FirestoreClassMapper;
import com.google.cloud.spring.data.firestore.mapping.FirestoreDefaultClassMapper;
import com.google.cloud.spring.data.firestore.mapping.FirestoreMappingContext;
import com.google.firestore.v1.FirestoreGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Firestore configuration for 'local' profile.
 *
 * - যদি FIRESTORE_EMULATOR_HOST পরিবেশ ভেরিয়েবল সেট থাকে → emulator-এ সংযুক্ত হবে
 * - অন্যথায় → Cloud Firestore (firestore.googleapis.com:443)-এ সংযুক্ত হবে
 *
 * আগে localhost:8081 হার্ডকোড করা ছিল, যার ফলে emulator বন্ধ থাকলে
 * "UNAVAILABLE: io exception" ত্রুটি হতো এবং login ব্যর্থ হতো।
 */
@Configuration
@Profile({"local", "test", "sandbox"})
public class FirestoreLocalConfig {

    private static final Logger log = LoggerFactory.getLogger(FirestoreLocalConfig.class);

    @org.springframework.beans.factory.annotation.Value("${spring.cloud.gcp.firestore.host:}")
    private String emulatorHost;

    /** Cloud Firestore-এর আসল gRPC endpoint */
    private static final String CLOUD_FIRESTORE_ENDPOINT = "firestore.googleapis.com:443";

    @Bean
    public FirestoreMappingContext firestoreMappingContext() {
        return new FirestoreMappingContext();
    }

    @Bean
    public FirestoreClassMapper firestoreClassMapper(FirestoreMappingContext mappingContext) {
        return new FirestoreDefaultClassMapper(mappingContext);
    }

    @Bean
    public ManagedChannel firestoreManagedChannel() {
        if (emulatorHost != null && !emulatorHost.isEmpty()) {
            log.info("Firestore: Connecting to EMULATOR at {}", emulatorHost);
            return ManagedChannelBuilder.forTarget(emulatorHost)
                    .usePlaintext()
                    .build();
        }
        
        log.info("Firestore: Connecting to CLOUD FIRESTORE at {}", CLOUD_FIRESTORE_ENDPOINT);
        return ManagedChannelBuilder.forTarget(CLOUD_FIRESTORE_ENDPOINT)
                .useTransportSecurity() // Cloud needs TLS
                .build();
    }

    @Bean
    public FirestoreGrpc.FirestoreStub firestoreGrpcStub(ManagedChannel channel) {
        return FirestoreGrpc.newStub(channel);
    }

    @org.springframework.beans.factory.annotation.Value("${spring.cloud.gcp.firestore.project-id:supremeai-a}")
    private String projectId;

    @Bean
    public FirestoreTemplate firestoreTemplate(
            FirestoreGrpc.FirestoreStub firestoreStub,
            FirestoreClassMapper classMapper,
            FirestoreMappingContext mappingContext) {
        String parentPath = String.format("projects/%s/databases/(default)/documents", projectId);
        return new FirestoreTemplate(firestoreStub, parentPath, classMapper, mappingContext);
    }
}
