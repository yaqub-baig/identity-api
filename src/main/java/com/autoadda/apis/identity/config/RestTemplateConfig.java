package com.autoadda.apis.identity.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;

@Configuration
@Slf4j
public class RestTemplateConfig {

    @Value("${http.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${http.proxy.port:0}")
    private int proxyPort;

    @Value("${dns.use.google:false}")
    private boolean useGoogleDNS;

    @EventListener(ApplicationReadyEvent.class)
    public void configureGoogleDNS() {
        if (useGoogleDNS) {
            log.info("Configuring JVM to use Google DNS (8.8.8.8, 8.8.4.4)");
            // Set Google DNS as the JVM's DNS servers
            System.setProperty("sun.net.spi.nameservice.nameservers", "8.8.8.8,8.8.4.4");
            System.setProperty("sun.net.spi.nameservice.provider.1", "dns,sun");
            log.info("Google DNS configured successfully");
        }
    }

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        // Configure proxy if settings are provided
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
            log.info("Configuring RestTemplate with HTTP proxy: {}:{}", proxyHost, proxyPort);
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            requestFactory.setProxy(proxy);
        } else {
            log.info("No HTTP proxy configured. Using direct connection.");
        }

        // Set connection and read timeouts (30 seconds)
        requestFactory.setConnectTimeout(30000);
        requestFactory.setReadTimeout(30000);

        return new RestTemplate(requestFactory);
    }
}
