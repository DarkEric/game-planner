package ru.ambryo.gameplannerback.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.ambryo.gameplannerback.service.TelegramNotificationService;

@Configuration
@ConditionalOnProperty(name = "telegram.bot.enabled", havingValue = "true")
public class TelegramBotConfig {

    /**
     * Опциональный SOCKS5 для исходящих запросов Telegram API (long polling и execute).
     * Включается только при {@code telegram.bot.proxy.enabled=true}.
     * Непустой {@code telegram.bot.proxy.username} включает SOCKS5-авторизацию через
     * {@link java.net.Authenticator} (на весь процесс JVM).
     */
    @Bean
    @ConditionalOnProperty(name = "telegram.bot.proxy.enabled", havingValue = "true")
    public DefaultBotOptions telegramSocks5BotOptions(
            @Value("${telegram.bot.proxy.host}") String host,
            @Value("${telegram.bot.proxy.port:1080}") int port,
            @Value("${telegram.bot.proxy.username:}") String proxyUsername,
            @Value("${telegram.bot.proxy.password:}") String proxyPassword) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException(
                    "telegram.bot.proxy.enabled=true requires non-empty telegram.bot.proxy.host");
        }
        String trimmedHost = host.trim();
        if (proxyUsername != null && !proxyUsername.isBlank()) {
            TelegramSocksProxySupport.installAuthenticatorIfNeeded(
                    trimmedHost, port, proxyUsername, proxyPassword);
        }
        DefaultBotOptions options = new DefaultBotOptions();
        options.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);
        options.setProxyHost(trimmedHost);
        options.setProxyPort(port);
        return options;
    }
    
    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramNotificationService telegramNotificationService) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        botsApi.registerBot(telegramNotificationService);
        
        // Логируем конфигурацию при старте
        telegramNotificationService.logConfiguration();
        
        return botsApi;
    }
}
