/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.delivery.webserver.http;

import com.djrapitops.plan.delivery.webserver.ResponseResolver;
import com.djrapitops.plan.delivery.webserver.configuration.WebserverConfiguration;
import com.djrapitops.plan.delivery.webserver.configuration.WebserverLogMessages;
import com.djrapitops.plan.exceptions.EnableException;
import net.playeranalytics.plugin.server.PluginLogger;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.SecuredRedirectHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.util.Optional;

@Singleton
public class JettyWebserver implements WebServer {

    private final PluginLogger logger;
    private final WebserverConfiguration webserverConfiguration;
    private final LegacyJettySSLContextLoader legacyJettySSLContextLoader;
    private final JettyRequestHandler jettyRequestHandler;
    private final ResponseResolver responseResolver;
    private final WebserverLogMessages webserverLogMessages;

    private int port;
    private boolean usingHttps;
    private Server webserver;

    @Inject
    public JettyWebserver(PluginLogger logger, WebserverConfiguration webserverConfiguration, LegacyJettySSLContextLoader legacyJettySSLContextLoader, JettyRequestHandler jettyRequestHandler, ResponseResolver responseResolver) {
        this.logger = logger;
        this.webserverConfiguration = webserverConfiguration;
        webserverLogMessages = webserverConfiguration.getWebserverLogMessages();
        this.legacyJettySSLContextLoader = legacyJettySSLContextLoader;
        this.jettyRequestHandler = jettyRequestHandler;
        this.responseResolver = responseResolver;
    }

    @Override
    public void enable() {
        if (isEnabled()) return;

        if (webserverConfiguration.isWebserverDisabled()) {
            webserverLogMessages.warnWebserverDisabledByConfig();
            return;
        }

        webserver = new Server();

        this.port = webserverConfiguration.getPort();

        HttpConfiguration configuration = new HttpConfiguration();
        Optional<SslContextFactory.Server> sslContext = getSslContextFactory();
        sslContext.ifPresent(ssl -> {
            configuration.setSecureScheme("https");
            configuration.setSecurePort(port);

            SecureRequestCustomizer serverNameIdentifierCheckSkipper = new SecureRequestCustomizer();
            serverNameIdentifierCheckSkipper.setSniHostCheck(false);
            serverNameIdentifierCheckSkipper.setSniRequired(false);
            configuration.addCustomizer(serverNameIdentifierCheckSkipper);

            usingHttps = true;
        });

        HttpConnectionFactory httpConnector = new HttpConnectionFactory(configuration);

        HTTP2CServerConnectionFactory http2CConnector = new HTTP2CServerConnectionFactory(configuration);
        http2CConnector.setConnectProtocolEnabled(true);


        ServerConnector connector = sslContext
                .map(sslContextFactory -> {
                    HTTP2ServerConnectionFactory http2Connector = new HTTP2ServerConnectionFactory(configuration);
                    http2Connector.setConnectProtocolEnabled(true);
                    ALPNServerConnectionFactory alpn = getAlpnServerConnectionFactory(httpConnector.getProtocol());

                    return new ServerConnector(webserver, sslContextFactory, alpn, httpConnector, http2Connector, http2CConnector);
                })
                .orElseGet(() -> {
                    webserverLogMessages.authenticationNotPossible();
                    return new ServerConnector(webserver, httpConnector, http2CConnector);
                });

        connector.setPort(port);
        webserver.addConnector(connector);

        if (usingHttps) {
            webserver.setHandler(new HandlerList(new SecuredRedirectHandler(), jettyRequestHandler));
        } else {
            webserver.setHandler(jettyRequestHandler);
        }

        try {
            webserver.start();
        } catch (Exception e) {
            throw new EnableException("Failed to start Jetty webserver: " + e.toString(), e);
        }

        webserverLogMessages.infoWebserverEnabled(getPort());

        responseResolver.registerPages();
    }

    private ALPNServerConnectionFactory getAlpnServerConnectionFactory(String protocol) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader pluginClassLoader = getClass().getClassLoader();
            // Jetty uses Thread context classloader, so we need to change to plugin classloader where the ALPNProcessor is.
            Thread.currentThread().setContextClassLoader(pluginClassLoader);

            Class.forName("org.eclipse.jetty.alpn.java.server.JDK9ServerALPNProcessor");
            // ALPN is protocol upgrade protocol required for upgrading http 1.1 connections to 2
            ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory("h2", "h2c", "http/1.1");
            alpn.setDefaultProtocol(protocol);

            return alpn;
        } catch (ClassNotFoundException ignored) {
            logger.warn("JDK9ServerALPNProcessor not found. ALPN is not available.");
            return null;
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private Optional<SslContextFactory.Server> getSslContextFactory() {
        String keyStorePath = webserverConfiguration.getKeyStorePath();
        if ("proxy".equals(keyStorePath)) {
            webserverLogMessages.authenticationUsingProxy();
            return Optional.empty();
        }

        if (!new File(keyStorePath).exists()) {
            webserverLogMessages.keystoreFileNotFound();
            return Optional.empty();
        }

        String storepass = webserverConfiguration.getKeyStorePassword();
        String keypass = webserverConfiguration.getKeyManagerPassword();
        String alias = webserverConfiguration.getAlias();

        if (keyStorePath.endsWith(".jks") && "DefaultPlanCert".equals(alias)) {
            logger.warn("You're using self-signed PlanCert.jks certificate included with Plan.jar (Considered legacy since 5.5), it has expired and can cause issues.");
            logger.info("Create new self-signed certificate using openssl:");
            logger.info("    openssl req -x509 -newkey rsa:4096 -keyout myKey.pem -out cert.pem -days 3650");
            logger.info("    openssl pkcs12 -export -out keyStore.p12 -inkey myKey.pem -in cert.pem -name alias -passout pass:<password> -passin pass:<password>");
            logger.info("Then change config settings to match.");
            logger.info("  SSL_certificate:");
            logger.info("      KeyStore_path: keyStore.p12");
            logger.info("      Key_pass: <password>");
            logger.info("      Store_pass: <password>");
            logger.info("      Alias: alias");
            return legacyJettySSLContextLoader.load(keyStorePath, storepass, keypass, alias);
        }

        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setSniRequired(false);

        sslContextFactory.setKeyStorePath(keyStorePath);
        sslContextFactory.setKeyStorePassword(storepass);
        sslContextFactory.setKeyManagerPassword(keypass);
        sslContextFactory.setCertAlias(alias);

        return Optional.of(sslContextFactory);
    }

    @Override
    public boolean isEnabled() {
        return webserver != null && (webserver.isStarting() || webserver.isStarted());
    }

    @Override
    public void disable() {
        try {
            if (webserver != null) webserver.stop();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getProtocol() {
        return isUsingHTTPS() ? "https" : "http";
    }

    @Override
    public boolean isUsingHTTPS() {
        return usingHttps;
    }

    @Override
    public boolean isAuthRequired() {
        return isUsingHTTPS() && webserverConfiguration.isAuthenticationEnabled();
    }

    @Override
    public int getPort() {
        return port;
    }
}
