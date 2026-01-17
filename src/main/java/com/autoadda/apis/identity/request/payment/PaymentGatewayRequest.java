package com.autoadda.apis.identity.request.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentGatewayRequest {

    private String subject;
    private String paySceneCode;
    private String payChannelCode;
    private String platformType;
    private BigDecimal amount;
    private GoodsDetail goodsDetail;
    private TerminalDetail terminalDetail;
    private String redirectUrl;
    private Integer expiryMints;
    private Integer userLeft;
    private Boolean isSendsms;
    private String mobno;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class GoodsDetail {
        private String body;
        private String categoriesTree;
        private String goodsCategory;
        private String goodsId;
        private String goodsName;
        private Price price;
        private Integer quantity;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Price {
            private String currency;
            private BigDecimal amount;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class TerminalDetail {
        private String merchantName;
        private String operatorId;
        private String storeId;
        private String terminalId;
        private String storeName;
    }
}
