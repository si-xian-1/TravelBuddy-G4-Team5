package com.TravelBuddy.controller.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.model.User;
import com.TravelBuddy.service.TravelerService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

class TravelerControllerTests {

    private static final int USER_ID = 42;

    // Adjust field names/values if your User model validation requires more.
    private static final String VALID_USER_JSON = """
            {
              "username": "test-traveler",
              "password": "test-password",
              "mobileNumber": "91234567",
              "hasConsentLocation": true
            }
            """;

    private TravelerService travelerService;
    private AuthenticationManager authenticationManager;
    private SecurityContextRepository securityContextRepository;
    private MockMvc mockMvc;
    private User currentUser;

    @BeforeEach
    void setUp() {
        travelerService = mock(TravelerService.class);
        authenticationManager = mock(AuthenticationManager.class);
        securityContextRepository = mock(SecurityContextRepository.class);

        TravelerController controller = new TravelerController(
                travelerService, authenticationManager, securityContextRepository);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        currentUser = new User();
        currentUser.setUserId(USER_ID);
        currentUser.setUsername("test-traveler");
        currentUser.setRole("TRAVELER");

        // Backs @AuthenticationPrincipal for the profile endpoints
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticatedToken(currentUser));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ---------- validation (@Valid on User) ----------

    static Stream<Arguments> invalidUserFields() {
        return Stream.of(
                Arguments.of("username", null),
                Arguments.of("username", ""),
                Arguments.of("username", "   "),
                Arguments.of("username", "a".repeat(51)),
                Arguments.of("mobileNumber", ""),
                Arguments.of("mobileNumber", "   "),
                Arguments.of("mobileNumber", "9123456a"),
                Arguments.of("mobileNumber", "123456789"),
                Arguments.of("hasConsentLocation", false),
                Arguments.of("emergencyContactName", "a".repeat(51)),
                Arguments.of("emergencyContactMobileNumber", "abc"),
                Arguments.of("emergencyContactMobileNumber", "123456789"),
                Arguments.of("password", "a".repeat(256)));
    }

    @ParameterizedTest(name = "signup rejects {0}={1}")
    @MethodSource("invalidUserFields")
    void signupRejectsInvalidFields(String field, Object value) throws Exception {
        mockMvc.perform(post("/api/travelers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWith(field, value)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(travelerService);
    }

    @ParameterizedTest(name = "updateProfile rejects {0}={1}")
    @MethodSource("invalidUserFields")
    void updateProfileRejectsInvalidFields(String field, Object value) throws Exception {
        mockMvc.perform(put("/api/travelers/updateProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWith(field, value)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(travelerService);
    }

    @Test
    void signupForwardsMissingMobileAndConsentToServiceBecauseBeanValidationAllowsNull() throws Exception {
        // @Pattern and @AssertTrue treat null as valid, so TravelerService is what rejects these.
        when(travelerService.createUser(any(User.class))).thenReturn(1);

        mockMvc.perform(post("/api/travelers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithout("mobileNumber", "hasConsentLocation")))
                .andExpect(status().isOk());

        verify(travelerService).createUser(any(User.class));
    }

    @Test
    void updateProfileAcceptsPartialBodyWithOnlyUsername() throws Exception {
        when(travelerService.updateUser(eq(USER_ID), any(User.class))).thenReturn(1);

        mockMvc.perform(put("/api/travelers/updateProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithout("password", "mobileNumber", "hasConsentLocation")))
                .andExpect(status().isOk());

        verify(travelerService).updateUser(eq(USER_ID), any(User.class));
    }

    @Test
    void updateProfileAcceptsEmptyEmergencyContactNumber() throws Exception {
        // The emergency contact pattern is ^\d*$, so clearing the number is allowed.
        when(travelerService.updateUser(eq(USER_ID), any(User.class))).thenReturn(1);

        mockMvc.perform(put("/api/travelers/updateProfile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWith("emergencyContactMobileNumber", "")))
                    .andExpect(status().isOk());
    }

    // ---------- response shape ----------

    @Test
    void myProfileDoesNotExposePasswordOrAuthorities() throws Exception {
        currentUser.setPassword("stored-hash");
        when(travelerService.getUser(USER_ID)).thenReturn(currentUser);

        mockMvc.perform(get("/api/travelers/myProfile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.username").value("test-traveler"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.authorities").doesNotExist());
    }

    // ---------- signup ----------

    @Test
    void signupCreatesUserAndReturnsSuccessMessage() throws Exception {
        when(travelerService.createUser(any(User.class))).thenReturn(1);

        mockMvc.perform(post("/api/travelers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("User created successfully"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(travelerService).createUser(captor.capture());
        assertEquals("test-traveler", captor.getValue().getUsername());
    }

    @Test
    void signupReturnsConflictWhenServiceReportsDuplicateUsername() throws Exception {
        when(travelerService.createUser(any(User.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use"));

        mockMvc.perform(post("/api/travelers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void signupReturnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/travelers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(travelerService);
    }

    // ---------- login ----------

    @Test
    void loginAuthenticatesPersistsContextAndReturnsUser() throws Exception {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authenticatedToken(currentUser));

        mockMvc.perform(post("/api/travelers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test-traveler"));

        ArgumentCaptor<Authentication> authCaptor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authCaptor.capture());
        assertEquals("test-traveler", authCaptor.getValue().getName());
        assertEquals("test-password", authCaptor.getValue().getCredentials());

        ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(securityContextRepository).saveContext(
                contextCaptor.capture(),
                any(HttpServletRequest.class),
                any(HttpServletResponse.class));
        assertEquals(currentUser, contextCaptor.getValue().getAuthentication().getPrincipal());
    }

    @Test
    void loginPropagatesAuthenticationFailureAndDoesNotPersistContext() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Standalone MockMvc has no security filter chain, so the exception surfaces
        // wrapped in a ServletException. In the real app, Spring Security maps it to 401.
        ServletException exception = assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/api/travelers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON)));

        assertInstanceOf(BadCredentialsException.class, exception.getCause());
        verifyNoInteractions(securityContextRepository);
    }

    // ---------- updateProfile ----------

    @Test
    void updateProfileUsesAuthenticatedUserIdAndReturnsSuccessMessage() throws Exception {
        when(travelerService.updateUser(eq(USER_ID), any(User.class))).thenReturn(1);

        mockMvc.perform(put("/api/travelers/updateProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("User updated successfully"));

        verify(travelerService).updateUser(eq(USER_ID), any(User.class));
    }

    @Test
    void updateProfileReturnsNotFoundWhenNoRowsUpdated() throws Exception {
        when(travelerService.updateUser(eq(USER_ID), any(User.class))).thenReturn(0);

        mockMvc.perform(put("/api/travelers/updateProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProfileReturnsConflictWhenServiceReportsDuplicateUsername() throws Exception {
        when(travelerService.updateUser(eq(USER_ID), any(User.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use"));

        mockMvc.perform(put("/api/travelers/updateProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    void updateProfileReturnsBadRequestWhenServiceRejectsPassword() throws Exception {
        when(travelerService.updateUser(eq(USER_ID), any(User.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password"));

        mockMvc.perform(put("/api/travelers/updateProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isBadRequest());
    }

    // ---------- myProfile ----------

    @Test
    void myProfileReturnsCurrentUsersDetails() throws Exception {
        when(travelerService.getUser(USER_ID)).thenReturn(currentUser);

        mockMvc.perform(get("/api/travelers/myProfile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test-traveler"));

        verify(travelerService).getUser(USER_ID);
    }

    @Test
    void myProfileReturnsNotFoundWhenServiceCannotFindUser() throws Exception {
        when(travelerService.getUser(USER_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Traveler not found"));

        mockMvc.perform(get("/api/travelers/myProfile"))
                .andExpect(status().isNotFound());
    }

    // ---------- deleteProfile ----------

    @Test
    void deleteProfileDeletesAuthenticatedUserAndReturnsSuccessMessage() throws Exception {
        when(travelerService.deleteUser(USER_ID)).thenReturn(1);

        mockMvc.perform(delete("/api/travelers/deleteProfile"))
                .andExpect(status().isOk())
                .andExpect(content().string("User account deleted"));

        verify(travelerService).deleteUser(USER_ID);
    }

    @Test
    void deleteProfileReturnsNotFoundWhenNoRowsDeleted() throws Exception {
        when(travelerService.deleteUser(USER_ID)).thenReturn(0);

        mockMvc.perform(delete("/api/travelers/deleteProfile"))
                .andExpect(status().isNotFound());
    }

    // ---------- helpers ----------

    private Authentication authenticatedToken(User principal) {
        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_TRAVELER")));
    }

    private static String bodyWith(String field, Object value) throws Exception {
        Map<String, Object> body = validBody();
        body.put(field, value);
        return MAPPER.writeValueAsString(body);
    }

    private static String bodyWithout(String... fields) throws Exception {
        Map<String, Object> body = validBody();
        for (String field : fields) {
            body.remove(field);
        }
        return MAPPER.writeValueAsString(body);
    }

    private static Map<String, Object> validBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", "test-traveler");
        body.put("password", "test-password");
        body.put("mobileNumber", "91234567");
        body.put("hasConsentLocation", true);
        return body;
    }
}