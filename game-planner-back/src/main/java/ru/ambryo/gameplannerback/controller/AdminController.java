package ru.ambryo.gameplannerback.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.ambryo.gameplannerback.service.CleanupService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {
    
    @Autowired
    private CleanupService cleanupService;
    
    /**
     * Ручной запуск очистки устаревших данных
     * В будущем можно добавить проверку прав администратора
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, String>> triggerCleanup() {
        try {
            cleanupService.manualCleanup();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cleanup completed successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Cleanup failed: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Получить информацию о конфигурации очистки
     */
    @GetMapping("/cleanup/info")
    public ResponseEntity<Map<String, Object>> getCleanupInfo() {
        Map<String, Object> info = cleanupService.getCleanupInfo();
        return ResponseEntity.ok(info);
    }
}
