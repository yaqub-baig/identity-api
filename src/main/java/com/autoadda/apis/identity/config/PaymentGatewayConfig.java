package com.autoadda.apis.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "payment.gateway")
@Getter
@Setter
public class PaymentGatewayConfig {

    private String baseUrl;
    private String createEndpoint = "/create";
    private String callbackUrl;
    private String callbackSecret;

    // Terminal details
    private String merchantName;
    private String operatorId;
    private String storeId;
    private String terminalId;
    private String storeName;

    // Goods details
    private String goodsCategory;
    private String categoriesTree;

    // Payment settings
    private Integer expiryMinutes = 15;
    private String currency = "AED";
    private String paySceneCode = "DYNQR";
    private String payChannelCode = "BANKCARD";
    private String platformType = "WEB";

    // Frontend redirect URLs
    private String frontendSuccessUrl;
    private String frontendFailureUrl;
}
