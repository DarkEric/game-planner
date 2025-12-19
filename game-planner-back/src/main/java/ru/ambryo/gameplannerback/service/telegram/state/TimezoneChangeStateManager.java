package ru.ambryo.gameplannerback.service.telegram.state;

import org.springframework.stereotype.Component;

/**
 * Менеджер состояний смены часового пояса
 */
@Component
public class TimezoneChangeStateManager extends AbstractStateManager<TimezoneChangeStateManager.TimezoneChangeState> {
    
    private static final long TIMEZONE_CHANGE_STATE_TIMEOUT_SECONDS = 300; // 5 минут таймаут состояния
    
    public enum TimezoneChangeState {
        WAITING_TIMEZONE
    }
    
    public TimezoneChangeStateManager() {
        super(TIMEZONE_CHANGE_STATE_TIMEOUT_SECONDS);
    }
}

