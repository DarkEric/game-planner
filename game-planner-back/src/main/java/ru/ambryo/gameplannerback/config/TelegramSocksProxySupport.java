package ru.ambryo.gameplannerback.config;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 * SOCKS5 с логином/паролем: библиотека telegrambots открывает {@link java.net.Socket}
 * через {@link java.net.Proxy.Type#SOCKS}; для авторизации JDK использует
 * {@link Authenticator#setDefault(Authenticator)} (действует на весь процесс JVM).
 * <p>
 * Для SOCKS5 JDK вызывает {@code getPasswordAuthentication()} с {@link Authenticator.RequestorType#PROXY},
 * при этом {@link #getRequestingHost()}/{@link #getRequestingPort()} часто относятся к
 * <b>целевому</b> хосту (например {@code api.telegram.org:443}), а не к {@code host:port} прокси.
 * Сравнивать их с настройками прокси нельзя — иначе возвращается {@code null} и получается
 * {@code SOCKS : authentication failed}.
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
                if (getRequestorType() != Authenticator.RequestorType.PROXY) {
                    return null;
                }
                String protocol = getRequestingProtocol();
                if (protocol != null) {
                    if (protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https")) {
                        return null;
                    }
                    if (!protocol.equalsIgnoreCase("socks")) {
                        return null;
                    }
                }
                return new PasswordAuthentication(user, passChars);
            }
        });
    }
}
