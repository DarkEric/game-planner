package ru.ambryo.gameplannerback.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.util.JwtUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserRepository userRepository;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Логируем только для запросов к защищенным эндпоинтам
            String requestPath = request.getRequestURI();
            if (requestPath.startsWith("/api-docs") || requestPath.startsWith("/swagger-ui") || requestPath.startsWith("/api/admin")) {
                logger.warn("Unauthenticated request to protected endpoint: {}", requestPath);
            }
            chain.doFilter(request, response);
            return;
        }
        
        try {
            final String jwt = authHeader.substring(7);
            final String username = jwtUtil.extractUsername(jwt);
            
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                User user = userRepository.findByUsername(username).orElse(null);
                
                if (user != null && jwtUtil.validateToken(jwt, username)) {
                    // Добавляем роли пользователя
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                    if (user.getIsAdmin() != null && user.getIsAdmin()) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        logger.debug("User {} (ID: {}) has admin role, added ROLE_ADMIN authority", 
                                user.getUsername(), user.getId());
                    } else {
                        logger.debug("User {} (ID: {}) is not admin (isAdmin: {})", 
                                user.getUsername(), user.getId(), user.getIsAdmin());
                    }
                    
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user, null, authorities
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    logger.debug("Authentication set for user {} with authorities: {}", 
                            user.getUsername(), authorities.stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .toList());
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }
        
        chain.doFilter(request, response);
    }
}

