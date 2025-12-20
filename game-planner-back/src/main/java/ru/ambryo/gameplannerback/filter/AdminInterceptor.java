package ru.ambryo.gameplannerback.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.service.AdminService;

@Component
public class AdminInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminInterceptor.class);
    
    @Autowired
    private AdminService adminService;
    
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        
        // Пропускаем публичные endpoints
        if (requestPath.equals("/api/admin/users/me/is-admin")) {
            return true;
        }
        
        // Проверяем аутентификацию
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            logger.warn("Unauthorized access attempt to admin endpoint: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return false;
        }
        
        // Проверяем, что principal - это User
        if (!(authentication.getPrincipal() instanceof User user)) {
            logger.warn("Invalid principal type for admin endpoint: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Forbidden\"}");
            return false;
        }

        // Проверяем права администратора
        if (!adminService.isAdmin(user)) {
            logger.warn("User '{}' (ID: {}) attempted to access admin endpoint without admin rights: {}", 
                    user.getUsername(), user.getId(), requestPath);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Forbidden: Admin rights required\"}");
            return false;
        }
        
        return true;
    }
}
