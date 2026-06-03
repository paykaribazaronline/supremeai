package com.supremeai.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.supremeai.model.ActivityLog;
import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.repository.UserRepository;
import com.supremeai.security.JwtUtil;
import com.supremeai.security.BruteForceProtectionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.env.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import reactor.core.publisher.Mono;
import com.supremeai.response.ApiResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AuthenticationControllerTest {AuthenticationControllerpublic AuthenticationControllerTest(AuthenticationController authenticationController, UserRepository userRepository, ActivityLogRepository activityLogRepository, JwtUtil jwtUtil, Environment env, com.google.cloud.spring.data.firestore.FirestoreTemplate firestoreTemplate, com.supremeai.service.AuthenticationService authenticationService, com.supremeai.service.ConfigService configService, com.supremeai.security.BruteForceProtectionService bruteForceProtectionService, MockedStatic<FirebaseAuth> firebaseAuthMock, FirebaseAuth firebaseAuth, AutoCloseable closeable) {
AuthenticationController    this.authenticationController = authenticationController;
AuthenticationController    this.userRepository = userRepository;
AuthenticationController    this.activityLogRepository = activityLogRepository;
AuthenticationController    this.jwtUtil = jwtUtil;
AuthenticationController    this.env = env;
AuthenticationController    this.firestoreTemplate = firestoreTemplate;
AuthenticationController    this.authenticationService = authenticationService;
AuthenticationController    this.configService = configService;
AuthenticationController    this.bruteForceProtectionService = bruteForceProtectionService;
AuthenticationController    this.firebaseAuthMock = firebaseAuthMock;
AuthenticationController    this.firebaseAuth = firebaseAuth;
AuthenticationController    this.closeable = closeable;
AuthenticationController}


    @InjectMocks






















    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        firebaseAuthMock = mockStatic(FirebaseAuth.class);
        firebaseAuth = mock(FirebaseAuth.class);
        firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
        when(jwtUtil.generateToken(anyString(), anyString())).thenReturn("mock-jwt-token");
        when(jwtUtil.generateAccessToken(anyString(), anyString())).thenReturn("mock-access-token");
        when(jwtUtil.generateRefreshToken(anyString(), anyString())).thenReturn("mock-refresh-token");
        when(env.getActiveProfiles()).thenReturn(new String[]{"test"});
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() throws Exception {
        firebaseAuthMock.close();
        closeable.close();
        SecurityContextHolder.clearContext();
    }

    @Test
    void testFirebaseLogin_Success_ExistingUser() throws Exception {
        String idToken = "valid-token";
        String uid = "test-uid";
        String email = "test@example.com";
        String name = "Test User";

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn(uid);
        when(token.getEmail()).thenReturn(email);
        Map<String, Object> claims = new HashMap<>();
        claims.put("name", name);
        when(token.getClaims()).thenReturn(claims);

        when(firebaseAuth.verifyIdToken(idToken)).thenReturn(token);

        User existingUser = new User(uid, email, name);
        existingUser.setTier(UserTier.FREE);
        
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("user", existingUser);
        mockData.put("isNewUser", false);
        mockData.put("status", "success");
        
        when(authenticationService.firebaseLogin(eq(idToken), anyString())).thenReturn(Mono.just(mockData));

        AuthenticationController.FirebaseLoginRequest loginRequest = new AuthenticationController.FirebaseLoginRequest(idToken);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        ApiResponse<Map<String, Object>> response = authenticationController.firebaseLogin(loginRequest, httpRequest).block();

        assertNotNull(response);
        Map<String, Object> responseData = response.getData();
        assertEquals("success", responseData.get("status"));
        assertEquals(false, responseData.get("isNewUser"));
        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) responseData.get("user");
        assertEquals(name, userMap.get("username"));
        assertEquals(email, userMap.get("email"));

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(httpRequest.getSession(false));
    }

    @Test
    void testFirebaseLogin_Success_NewUser() throws Exception {
        String idToken = "new-token";
        String uid = "new-uid";
        String email = "new@example.com";

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn(uid);
        when(token.getEmail()).thenReturn(email);
        when(token.getClaims()).thenReturn(new HashMap<>());

        when(firebaseAuth.verifyIdToken(idToken)).thenReturn(token);

        User newUser = new User(uid, email, "new");
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("user", newUser);
        mockData.put("isNewUser", true);
        mockData.put("status", "success");
        
        when(authenticationService.firebaseLogin(eq(idToken), anyString())).thenReturn(Mono.just(mockData));

        AuthenticationController.FirebaseLoginRequest loginRequest = new AuthenticationController.FirebaseLoginRequest(idToken);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        ApiResponse<Map<String, Object>> response = authenticationController.firebaseLogin(loginRequest, httpRequest).block();

        assertNotNull(response);
        Map<String, Object> responseData = response.getData();
        assertEquals("success", responseData.get("status"));
        assertEquals(true, responseData.get("isNewUser"));
    }

    @Test
    void testFirebaseLogin_Success_Admin() throws Exception {
        String idToken = "admin-token";
        String uid = "admin-uid";
        String email = "admin@example.com";

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn(uid);
        when(token.getEmail()).thenReturn(email);
        Map<String, Object> claims = new HashMap<>();
        claims.put("admin", true);
        when(token.getClaims()).thenReturn(claims);

        when(firebaseAuth.verifyIdToken(idToken)).thenReturn(token);

        User adminUser = new User(uid, email, "admin");
        adminUser.setTier(UserTier.ADMIN);
        Map<String, Object> mockData = new HashMap<>();
        mockData.put("user", adminUser);
        mockData.put("isNewUser", false);
        mockData.put("status", "success");
        
        when(authenticationService.firebaseLogin(eq(idToken), anyString())).thenReturn(Mono.just(mockData));

        AuthenticationController.FirebaseLoginRequest loginRequest = new AuthenticationController.FirebaseLoginRequest(idToken);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        ApiResponse<Map<String, Object>> response = authenticationController.firebaseLogin(loginRequest, httpRequest).block();

        assertNotNull(response);
        Map<String, Object> responseData = response.getData();
        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) responseData.get("user");
        assertEquals("admin", userMap.get("role"));
        assertEquals("ADMIN", userMap.get("tier"));
    }

    @Test
    void testFirebaseLogin_Failure_InvalidToken() throws Exception {
        String idToken = "invalid-token";
        when(authenticationService.firebaseLogin(eq(idToken), anyString()))
            .thenReturn(Mono.error(new RuntimeException("Authentication failed")));

        AuthenticationController.FirebaseLoginRequest loginRequest = new AuthenticationController.FirebaseLoginRequest(idToken);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        ApiResponse<Map<String, Object>> response = authenticationController.firebaseLogin(loginRequest, httpRequest).block();

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.getError().contains("Authentication failed"));
    }

    @Test
    void testLogout_Success() throws Exception {
        String uid = "test-uid";
        User user = new User(uid, "test@example.com", "Test User");
        
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        UsernamePasswordAuthenticationToken auth = 
            new UsernamePasswordAuthenticationToken(uid, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        context.setAuthentication(auth);
        
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        httpRequest.setSession(session);
        SecurityContextHolder.setContext(context);

        ApiResponse<String> response = authenticationController.logout(httpRequest).block();

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Logged out", response.getData());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(session.isInvalid());
    }
}
