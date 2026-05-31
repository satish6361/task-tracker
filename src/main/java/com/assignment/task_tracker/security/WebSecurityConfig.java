package com.assignment.task_tracker.security;

import com.assignment.task_tracker.dto.ErrorResponse;
import com.assignment.task_tracker.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;
    private final UserService userServiceImp;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver handlerExceptionResolver;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrfConfig -> csrfConfig.disable())
                .sessionManagement(
                        sessionConfig -> sessionConfig.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Public
                        .requestMatchers(HttpMethod.POST, "/auth/signup").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()

                        // Own profile — any authenticated user
                        .requestMatchers(HttpMethod.GET,   "/users/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/users/me").authenticated()

                        // My tasks shortcut — any authenticated user
                        .requestMatchers(HttpMethod.GET, "/users/me/tasks").authenticated()
                        .requestMatchers(HttpMethod.GET, "/notifications/stream").authenticated()

                        // Org management — ADMIN only
                        .requestMatchers(HttpMethod.GET,   "/organizations").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/organizations").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/organizations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/organizations/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/organizations/*/members").hasRole("ADMIN")

                        // Org user management — ADMIN only
                        // (GET /organizations/:orgId is open to any org member — enforced in service)
                        .requestMatchers(HttpMethod.GET,
                                "/organizations/*/users",
                                "/organizations/*/users/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH,  "/organizations/*/users/*/roles").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/organizations/*/users/*").hasRole("ADMIN")

                        // Projects — GET open to org members, mutations need MANAGER+
                        // Fine-grained role checks (e.g. "is this user in this org?") are in the service layer
                        .requestMatchers(HttpMethod.GET, "/organizations/*/projects/**").authenticated()
                        .requestMatchers(HttpMethod.POST,   "/organizations/*/projects").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PATCH,  "/organizations/*/projects/*").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/organizations/*/projects/*").hasAnyRole("ADMIN", "MANAGER")


                        //Users
                        .requestMatchers(HttpMethod.PATCH, "/users/*/roles").hasRole("ADMIN")

                        // Project members
                        .requestMatchers(HttpMethod.GET,
                                "/organizations/*/projects/*/members").authenticated()
                        .requestMatchers(HttpMethod.POST,
                                "/organizations/*/projects/*/members").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE,
                                "/organizations/*/projects/*/members/*").hasAnyRole("ADMIN", "MANAGER")

                        // Tasks — status update is open (assignee check is in the service)
                        .requestMatchers(HttpMethod.GET, "/projects/*/tasks/analytics").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET,  "/projects/*/tasks/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/projects/*/tasks").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PATCH,"/projects/*/tasks/*/status").hasAnyRole("ADMIN", "MANAGER", "MEMBER")
                        .requestMatchers(HttpMethod.PATCH,"/projects/*/tasks/*").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE,"/projects/*/tasks/*").hasAnyRole("ADMIN", "MANAGER")

                        // Comments — any org member can comment
                        .requestMatchers("/tasks/*/comments/**").authenticated()

                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(accessDeniedHandler())
                        .authenticationEntryPoint(authenticationEntryPoint())
                );
        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder getPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:3000",
                "http://127.0.0.1:3000"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager getAuthenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            handlerExceptionResolver.resolveException(request, response, null, accessDeniedException);
        };
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> {
            ErrorResponse error = new ErrorResponse("UNAUTHORIZED", "Authentication required");

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            new ObjectMapper().writeValue(response.getOutputStream(), error);
        };
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return userServiceImp;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(getPasswordEncoder());
        provider.setUserDetailsService(userServiceImp);
        return provider;
    }
}
