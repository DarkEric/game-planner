package ru.ambryo.gameplannerback.service.telegram.util;

/**
 * Утилита для валидации данных
 */
public class TelegramValidationUtils {
    
    /**
     * Проверяет валидность email адреса
     * @param email email для проверки
     * @return true если email валиден
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        // Простая проверка формата email
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}

