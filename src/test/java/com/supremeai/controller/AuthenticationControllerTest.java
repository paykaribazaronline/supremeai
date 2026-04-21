package com.supremeai.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.supremeai.model.ActivityLog;
import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AuthenticationControllerTest {

    @InjectMocks
    private AuthenticationController authenticationController;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivityLogRepository activityLogRepository;

    private MockedStatic<FirebaseAuth> firebaseAuthMock;
    private FirebaseAuth firebaseAuth;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        firebaseAuthMock = mockStatic(FirebaseAuth.class);
        firebaseAuth = mock(FirebaseAuth.class);
        firebaseAuthMock.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
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
        when(userRepository.findByFirebaseUid(uid)).thenReturn(Mono.just(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(existingUser));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.empty());

        Map<String, String> loginRequest = Map.of("idToken", idToken);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        Map<String, Object> response = authenticationController.firebaseLogin(loginRequest, httpRequest);

        assertEquals("success", response.get("status"));
        assertEquals(false, response.get("isNewUser"));
        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) response.get("user");
        assertEquals(name, userMap.get("username"));
        assertEquals(email, userMap.get("email"));

        verify(userRepository).findByFirebaseUid(uid);
        verify(userRepository).save(any(User.class));
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

        when(userRepository.findByFirebaseUid(uid)).thenReturn(Mono.empty());
        User newUser = new User(uid, email, "new");
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(newUser));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.empty());

        Map<String, String> loginRequest = Map.of("idToken", idToken);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        Map<String, Object> response = authenticationController.firebaseLogin(loginRequest, httpRequest);

        assertEquals("success", response.get("status"));
        assertEquals(true, response.get("isNewUser"));

        verify(userRepository).findByFirebaseUid(uid);
        verify(userRepository).save(any(User.class));
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
        when(userRepository.findByFirebaseUid(uid)).thenReturn(Mono.just(adminUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(adminUser));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.empty());

        Map<String, String> loginRequest = Map.of("idToken", idToken);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        Map<String, Object> response = authenticationController.firebaseLogin(loginRequest, httpRequest);

        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) response.get("user");
        assertEquals("admin", userMap.get("role"));
        assertEquals("ADMIN", userMap.get("tier"));
    }

    @Test
    void testFirebaseLogin_Failure_InvalidToken() throws Exception {
        String idToken = "invalid-token";
        when(firebaseAuth.verifyIdToken(idToken)).thenThrow(mock(FirebaseAuthException.class));

        Map<String, String> loginRequest = Map.of("idToken", idToken);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();

        Map<String, Object> response = authenticationController.firebaseLogin(loginRequest, httpRequest);

        assertEquals("error", response.get("status"));
        assertTrue(response.get("message").toString().contains("Auth failed"));
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

        when(userRepository.findByFirebaseUid(uid)).thenReturn(Mono.just(user));
        when(activityLogRepository.save(any(ActivityLog.class))).thenReturn(Mono.empty());

        Map<String, Object> response = authenticationController.logout(httpRequest);

        assertEquals("success", response.get("status"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(session.isInvalid());
        verify(activityLogRepository).save(any(ActivityLog.class));
    }
}
