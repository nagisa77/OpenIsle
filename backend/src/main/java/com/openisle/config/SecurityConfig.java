package com.openisle.config;

import com.openisle.service.JwtService;
import com.openisle.service.UserVisitService;
import com.openisle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AccessDeniedHandler customAccessDeniedHandler;
    private final UserVisitService userVisitService;
    @Value("${app.website-url:https://www.open-isle.com}")
    private String websiteUrl;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .<UserDetails>map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPassword())
                        .authorities(user.getRole().name())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, PasswordEncoder passwordEncoder, UserDetailsService userDetailsService) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder)
                .and()
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(
                "http://127.0.0.1:8080",
                "http://127.0.0.1:3000",
                "http://127.0.0.1",
                "http://localhost:8080",
                "http://localhost:3000",
                "http://localhost",
                "http://30.211.97.238:3000",
                "http://30.211.97.238",
                "http://192.168.7.70",
                "http://192.168.7.70:8080",
                websiteUrl,
                websiteUrl.replace("://www.", "://")
        ));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())         // 让 Spring 自带 CorsFilter 处理预检
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(eh -> eh.accessDeniedHandler(customAccessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/comments/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/tags/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/config/**").permitAll()
                    .requestMatchers(HttpMethod.POST,"/api/auth/google").permitAll()
                    .requestMatchers(HttpMethod.POST,"/api/auth/reason").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/search/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/users/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/push/public-key").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/reaction-types").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/activities/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/sitemap.xml").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/categories/**").hasAuthority("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/tags/**").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasAuthority("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/tags/**").hasAuthority("ADMIN")
                    .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                    .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(userVisitFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public OncePerRequestFilter jwtAuthenticationFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                // 让预检请求直接通过
                if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                    filterChain.doFilter(request, response);
                    return;
                }
                String authHeader = request.getHeader("Authorization");
                String uri = request.getRequestURI();

                boolean publicGet = "GET".equalsIgnoreCase(request.getMethod()) &&
                        (uri.startsWith("/api/posts") || uri.startsWith("/api/comments") ||
                         uri.startsWith("/api/categories") || uri.startsWith("/api/tags") ||
                        uri.startsWith("/api/search") || uri.startsWith("/api/users") ||
                         uri.startsWith("/api/reaction-types") || uri.startsWith("/api/config") ||
                         uri.startsWith("/api/activities") || uri.startsWith("/api/push/public-key") ||
                         uri.startsWith("/api/sitemap.xml"));

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    try {
                        String username = jwtService.validateAndGetSubject(token);
                        UserDetails userDetails = userDetailsService().loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authToken);
                    } catch (Exception e) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\": \"Invalid or expired token\"}");
                        return;
                    }
                } else if (!uri.startsWith("/api/auth") && !publicGet) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Missing token\"}");
                    return;
                }

                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    public OncePerRequestFilter userVisitFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !(auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
                    userVisitService.recordVisit(auth.getName());
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}
