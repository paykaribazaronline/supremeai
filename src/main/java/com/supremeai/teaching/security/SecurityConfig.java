package com.supremeai.teaching.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // ১. অ্যাডমিন প্যানেল এবং অ্যাডমিন এপিআই সুরক্ষিত করা
                .requestMatchers("/admin.html").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // ২. ফ্রন্টএন্ড এবং বাকি সবকিছু সবার জন্য খোলা রাখা
                .requestMatchers("/", "/index.html", "/customer.html", "/login.html", "/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/config/**").permitAll()
                .requestMatchers("/api/status/**").permitAll()
                
                // ৩. ডিফল্টভাবে বাকি সবকিছু অথেনটিকেটেড হতে হবে (অথবা আপনি চাইলে permitAll() দিতে পারেন যদি একদম সিম্পল চান)
                .anyRequest().permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}
