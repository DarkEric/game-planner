package ru.ambryo.gameplannerback.config;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * SOCKS5 с логином/паролем: библиотека telegrambots открывает {@link java.net.Socket}
 * через {@link java.net.Proxy.Type#SOCKS}; для авторизации JDK использует
 * {@link Authenticator#setDefault(Authenticator)} (действует на весь процесс JVM).
 * <p>
 * В {@code java.net.SocksSocketImpl} вызывается overload {@code requestPasswordAuthentication}
 * без {@code RequestorType};
 * после {@code reset()} в {@link Authenticator} тип остаётся {@link Authenticator.RequestorType#SERVER},
 * не {@code PROXY}. Нельзя требовать только {@code PROXY}. Хост/порт в запросе — часто
 * целевой сервер ({@code api.telegram.org:443}), а не прокси.
 */
final class TelegramSocksProxySupport {

    private TelegramSocksProxySupport() {
    }

    static void installAuthenticatorIfNeeded(String proxyHost, int proxyPort, String username, String password) {
        if (username == null || username.isBlank()) {
            return;
        }
        final String user = username.trim();
        final char[] passChars = password != null ? password.toCharArray() : new char[0];
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                String protocol = getRequestingProtocol();
                String prompt = getRequestingPrompt();
                boolean socks = false;
                if (protocol != null && !protocol.isBlank()) {
                    String pl = protocol.toLowerCase();
                    if (pl.contains("sock")) {
                        socks = true;
                    } else if (pl.equals("http") || pl.equals("https")) {
                        return null;
                    }
                }
                if (!socks && prompt != null && prompt.toLowerCase().contains("sock")) {
                    socks = true;
                }
                if (!socks) {
                    return null;
                }
                return new PasswordAuthentication(user, passChars);
            }
        });
    }
}
