package com.TravelBuddy.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@EnableWebSecurity
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Configures security filters, endpoint authorizations, session management, CSRF protection, and entry points.
     * @param http security to configure
     * @param securityContextRepository to persist the security context
     * @return configured security
     * @throws Exception if a security configuration error occurs
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            SecurityContextRepository securityContextRepository) throws Exception {
        http
                .securityContext(security -> security
                        .securityContextRepository(
                                securityContextRepository))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId()))
                .authorizeHttpRequests((authz) -> authz
                        // Allow all users to view error pages
                        .requestMatchers("/error").permitAll()
                        // Allow all users to access sign up and login
                        .requestMatchers(HttpMethod.POST, "/api/travelers/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/travelers/signup").permitAll()
                        // Requires authentication to access profile management feature
                        .requestMatchers(HttpMethod.GET, "/api/travelers/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/travelers/*").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/travelers/*").authenticated()
                        // Requires authentication to access itinerary management feature
                        .requestMatchers(HttpMethod.POST, "/api/itinerary/").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/itinerary/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/itinerary/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/itinerary/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/itinerary/**").authenticated()
                        // Requires authentication to use the location search
                        .requestMatchers(HttpMethod.GET, "/api/location").authenticated()
                        // Frontend pages that requires authentication
                        .requestMatchers(HttpMethod.GET,
                                "/",
                                "/allItineraries",
                                "/myProfile",
                                "/itinerary/**")
                        .authenticated()
                        .anyRequest().permitAll())
                // Any attempt to access pages when user is not authenticated yet will redirect to login page
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
                .csrf(csrf -> csrf.disable())
                // Set up logout api and invalidate user session
                .logout(logout -> logout
                        .logoutUrl("/api/travelers/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"));

        return http.build();
    }

    /**
     * Exposes the AuthenticationManager bean from the authentication configuration.
     * @param configuration the authentication configuration
     * @return
     * @throws Exception if unable to obtain the authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Configures the password hashing algorithm using PBKDF2.
     * @return
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}