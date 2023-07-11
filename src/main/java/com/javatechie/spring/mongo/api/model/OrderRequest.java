package com.javatechie.spring.mongo.api.model;

import lombok.*;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "ORDER_REQUEST")
public class OrderRequest {
    private String exchange;
    private String tradingSymbol;
    private String transactionType;
    private String orderType;
    private String quantity;
    private String price;
    private String product;
    private String validity;
    private String disclosedQuantity;
    private String triggerPrice;
    private String squareoff;
    private String stoploss;
    private String trailingStoploss;
    private String userId;

}
