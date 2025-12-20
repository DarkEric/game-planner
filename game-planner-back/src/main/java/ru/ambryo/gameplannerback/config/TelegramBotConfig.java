package ru.ambryo.gameplannerback.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.ambryo.gameplannerback.service.TelegramNotificationService;

@Configuration
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true")
public class TelegramBotConfig {
    
    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramNotificationService telegramNotificationService) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(telegramNotificationService);
        
        // Логируем конфигурацию при старте
        telegramNotificationService.logConfiguration();
        
        return botsApi;
    }
}
