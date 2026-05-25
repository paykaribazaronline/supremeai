package com.supremeai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.CustomConversions.StoreConversions;
import com.google.cloud.Timestamp;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.CustomConversions.StoreConversions;

@Configuration
public class FirestoreConverterConfig {

    @Bean
    public CustomConversions firestoreCustomConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new LocalDateTimeToTimestampConverter());
        converters.add(new TimestampToLocalDateTimeConverter());
        return new CustomConversions(StoreConversions.NONE, converters);
    }

    static class LocalDateTimeToTimestampConverter implements Converter<LocalDateTime, Timestamp> {
        @Override
        public Timestamp convert(LocalDateTime source) {
            return Timestamp.of(Date.from(source.atZone(ZoneId.systemDefault()).toInstant()));
        }
    }

    static class TimestampToLocalDateTimeConverter implements Converter<Timestamp, LocalDateTime> {
        @Override
        public LocalDateTime convert(Timestamp source) {
            return LocalDateTime.ofInstant(source.toDate().toInstant(), ZoneId.systemDefault());
        }
    }
}
