package com.supremeai.config;

import com.google.cloud.spring.data.firestore.FirestoreTemplate;
import com.google.cloud.spring.data.firestore.mapping.FirestoreClassMapper;
import com.google.cloud.spring.data.firestore.mapping.FirestoreDefaultClassMapper;
import com.google.cloud.spring.data.firestore.mapping.FirestoreMappingContext;
import com.google.firestore.v1.FirestoreGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class FirestoreLocalConfig {

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
        return ManagedChannelBuilder.forTarget("localhost:8081")
                .usePlaintext()
                .build();
    }

    @Bean
    public FirestoreGrpc.FirestoreStub firestoreGrpcStub(ManagedChannel channel) {
        return FirestoreGrpc.newStub(channel);
    }

    @Bean
    public FirestoreTemplate firestoreTemplate(
            FirestoreGrpc.FirestoreStub firestoreStub,
            FirestoreClassMapper classMapper,
            FirestoreMappingContext mappingContext) {
        return new FirestoreTemplate(firestoreStub, "test-project", classMapper, mappingContext);
    }
}
