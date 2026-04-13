package ru.ambryo.gameplannerback.config;

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.UnknownHostException;

/**
 * SOCKS5 с логином/паролем: библиотека telegrambots открывает {@link java.net.Socket}
 * через {@link java.net.Proxy.Type#SOCKS}; для авторизации JDK использует
 * {@link Authenticator#setDefault(Authenticator)} (действует на весь процесс JVM).
 */
final class TelegramSocksProxySupport {

    private TelegramSocksProxySupport() {
    }

    static void installAuthenticatorIfNeeded(String proxyHost, int proxyPort, String username, String password) {
        if (username == null || username.isBlank()) {
            return;
        }
        final String host = proxyHost.trim();
        final int port = proxyPort;
        final String user = username.trim();
        final char[] passChars = password != null ? password.toCharArray() : new char[0];
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                if (getRequestorType() != Authenticator.RequestorType.PROXY) {
                    return null;
                }
                if (getRequestingPort() != port) {
                    return null;
                }
                String requestingHost = getRequestingHost();
                if (!hostsMatch(host, requestingHost)) {
                    return null;
                }
                return new PasswordAuthentication(user, passChars);
            }
        });
    }

    private static boolean hostsMatch(String configuredHost, String requestingHost) {
        if (requestingHost == null) {
            return false;
        }
        String req = requestingHost.trim();
        if (configuredHost.equalsIgnoreCase(req)) {
            return true;
        }
        try {
            InetAddress configured = InetAddress.getByName(configuredHost);
            InetAddress actual = InetAddress.getByName(req);
            return configured.equals(actual);
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
