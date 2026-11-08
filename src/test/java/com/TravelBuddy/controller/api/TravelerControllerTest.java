package com.TravelBuddy.controller.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.TravelBuddy.model.User;
import com.TravelBuddy.service.TravelerService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class TravelerControllerTest {

    private static final int USER_ID = 42;

    private static final String VALID_USER_JSON = """
            {
              "username": "test-traveler",
              "password": "test-password",
              "mobileNumber": "91234567",
              "hasConsentLocation": true
            }
            """;

    @Mock
    private TravelerService travelerService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @InjectMocks
    private TravelerController travelerController;

    private MockMvc mockMvc;
    private User currentUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(travelerController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        currentUser = new User();
        currentUser.setUserId(USER_ID);
        currentUser.setUsername("test-traveler");
        currentUser.setRole("TRAVELER");

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                currentUser, null, List.of(new SimpleGrantedAuthority("ROLE_TRAVELER"))));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Registering a new traveler profile creates the account successfully")
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
    @DisplayName("Registering with invalid user details is rejected")
    void signupRejectsInvalidFields() throws Exception {
        String invalidJson = """
                {
                  "username": "",
                  "password": "test-password",
                  "mobileNumber": "invalid"
                }
                """;

        mockMvc.perform(post("/api/travelers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(travelerService);
    }

    @Test
    @DisplayName("Registering without contact number or location consent is permitted during account creation")
    void signupForwardsMissingMobileAndConsentToServiceBecauseBeanValidationAllowsNull() throws Exception {
        when(travelerService.createUser(any(User.class))).thenReturn(1);

        String json = """
                {
                  "username": "test-traveler",
                  "password": "test-password"
                }
                """;

        mockMvc.perform(post("/api/travelers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(travelerService).createUser(any(User.class));
    }

    @Test
    @DisplayName("Registering without location consent returns bad request when rejected by registration rules")
    void signupReturnsBadRequestWhenServiceRejectsMissingConsent() throws Exception {
        when(travelerService.createUser(any(User.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid consent location"));

        String json = """
                {
                  "username": "test-traveler",
                  "password": "test-password",
                  "mobileNumber": "91234567",
                  "hasConsentLocation": false
                }
                """;

        mockMvc.perform(post("/api/travelers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Registering with a username that is already taken is rejected as a conflict")
    void signupReturnsConflictWhenServiceReportsDuplicateUsername() throws Exception {
        when(travelerService.createUser(any(User.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use"));

        mockMvc.perform(post("/api/travelers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Registering with an unreadable or corrupted request is rejected")
    void signupReturnsBadRequestForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/travelers/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(travelerService);
    }

    @Test
    @DisplayName("Signing in with valid traveler credentials starts a session and returns the profile")
    void loginAuthenticatesPersistsContextAndReturnsUser() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                currentUser, null, List.of(new SimpleGrantedAuthority("ROLE_TRAVELER")));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(auth);

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
    @DisplayName("Signing in with invalid credentials fails and prevents session creation")
    void loginPropagatesAuthenticationFailureAndDoesNotPersistContext() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        ServletException exception = assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/api/travelers/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON)));

        assertInstanceOf(BadCredentialsException.class, exception.getCause());
        verifyNoInteractions(securityContextRepository);
    }

    @Test
    @DisplayName("Updating traveler profile details saves changes and refreshes active login session")
    void updateProfileUsesAuthenticatedUserIdAndReturnsSuccessMessage() throws Exception {
        when(travelerService.updateUser(eq(USER_ID), any(User.class))).thenReturn(1);

        mockMvc.perform(put("/api/travelers/updateProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("User updated successfully"));

        verify(travelerService).updateUser(eq(USER_ID), any(User.class));
        verify(securityContextRepository).saveContext(
                any(SecurityContext.class),
                any(HttpServletRequest.class),
                any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("Updating profile with invalid user details is rejected")
    void updateProfileRejectsInvalidFields() throws Exception {
        String invalidJson = """
                {
                  "username": "",
                  "mobileNumber": "not-a-number"
                }
                """;

        mockMvc.perform(put("/api/travelers/updateProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(travelerService);
    }

    @Test
    @DisplayName("Updating account with only a new username succeeds")
    void updateProfileAcceptsPartialBodyWithOnlyUsername() throws Exception {
        when(travelerService.updateUser(eq(USER_ID), any(User.class))).thenReturn(1);

        String json = """
                {
                  "username": "updated-username"
                }
                """;

        mockMvc.perform(put("/api/travelers/updateProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(travelerService).updateUser(eq(USER_ID), any(User.class));
    }

    @Test
    @DisplayName("Updating profile allows withdrawing location consent")
    void updateProfileAllowsWithdrawingLocationConsent() throws Exception {
        when(travelerService.updateUser(eq(USER_ID), any(User.class))).thenReturn(1);

        String json = """
                {
                  "username": "test-traveler",
                  "hasConsentLocation": false
                }
                """;

        mockMvc.perform(put("/api/travelers/updateProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(travelerService).updateUser(eq(USER_ID), any(User.class));
    }

    @Test
    @DisplayName("Updating profile to clear the emergency contact number succeeds")
    void updateProfileAcceptsEmptyEmergencyContactNumber() throws Exception {
        when(travelerService.updateUser(eq(USER_ID), any(User.class))).thenReturn(1);

        String json = """
                {
                  "username": "test-traveler",
                  "emergencyContactMobileNumber": ""
                }
                """;

        mockMvc.perform(put("/api/travelers/updateProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Updating an unknown or missing traveler profile cannot be found")
    void updateProfileReturnsNotFoundWhenNoRowsUpdated() throws Exception {
        when(travelerService.updateUser(eq(USER_ID), any(User.class))).thenReturn(0);

        mockMvc.perform(put("/api/travelers/updateProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Updating to a username that is already taken by another traveler is rejected")
    void updateProfileReturnsConflictWhenServiceReportsDuplicateUsername() throws Exception {
        when(travelerService.updateUser(eq(USER_ID), any(User.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username is already in use"));

        mockMvc.perform(put("/api/travelers/updateProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Updating profile with an invalid password format is rejected")
    void updateProfileReturnsBadRequestWhenServiceRejectsPassword() throws Exception {
        when(travelerService.updateUser(eq(USER_ID), any(User.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid password"));

        mockMvc.perform(put("/api/travelers/updateProfile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Changing password with matching confirmation updates account password successfully")
    void changePasswordUpdatesPasswordAndReturnsSuccessMessage() throws Exception {
        String requestJson = """
                {
                  "newPassword": "newSecretPassword123",
                  "confirmPassword": "newSecretPassword123"
                }
                """;

        mockMvc.perform(put("/api/travelers/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().string("Password changed successfully"));

        verify(travelerService).changePassword(USER_ID, "newSecretPassword123", "newSecretPassword123");
    }

    @Test
    @DisplayName("Changing password with empty or missing inputs is rejected")
    void changePasswordRejectsBlankPasswords() throws Exception {
        String requestJson = """
                {
                  "newPassword": "   ",
                  "confirmPassword": ""
                }
                """;

        mockMvc.perform(put("/api/travelers/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(travelerService);
    }

    @Test
    @DisplayName("Changing password when confirmation does not match the new password is rejected")
    void changePasswordReturnsBadRequestWhenServiceThrowsException() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match"))
                .when(travelerService).changePassword(USER_ID, "pass1", "pass2");

        String requestJson = """
                {
                  "newPassword": "pass1",
                  "confirmPassword": "pass2"
                }
                """;

        mockMvc.perform(put("/api/travelers/changePassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Viewing own profile returns the authenticated traveler information")
    void myProfileReturnsCurrentUsersDetails() throws Exception {
        when(travelerService.getUser(USER_ID)).thenReturn(currentUser);

        mockMvc.perform(get("/api/travelers/myProfile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("test-traveler"));

        verify(travelerService).getUser(USER_ID);
    }

    @Test
    @DisplayName("Viewing profile of an account that no longer exists cannot be found")
    void myProfileReturnsNotFoundWhenServiceCannotFindUser() throws Exception {
        when(travelerService.getUser(USER_ID))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Traveler not found"));

        mockMvc.perform(get("/api/travelers/myProfile"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deleting own profile closes and removes traveler account successfully")
    void deleteProfileDeletesAuthenticatedUserAndReturnsSuccessMessage() throws Exception {
        when(travelerService.deleteUser(USER_ID)).thenReturn(1);

        mockMvc.perform(delete("/api/travelers/deleteProfile"))
                .andExpect(status().isOk())
                .andExpect(content().string("User account deleted"));

        verify(travelerService).deleteUser(USER_ID);
    }

    @Test
    @DisplayName("Deleting an account that does not exist cannot be found")
    void deleteProfileReturnsNotFoundWhenNoRowsDeleted() throws Exception {
        when(travelerService.deleteUser(USER_ID)).thenReturn(0);

        mockMvc.perform(delete("/api/travelers/deleteProfile"))
                .andExpect(status().isNotFound());
    }
}