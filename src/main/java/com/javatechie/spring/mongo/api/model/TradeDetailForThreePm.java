package com.javatechie.spring.mongo.api.model;

import lombok.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "TRADE_DETAILS_FOR_THREE_PM")
public class TradeDetailForThreePm {
    @Id
    private String id;
    private Map<String, Double> ceLeg;
    private Map<String, Double> peLeg;
    private double ceLegEntry;
    private double ceLegTarget;
    private double ceLegSl;
    private double peLegEntry;
    private double peLegTarget;
    private double peLegSl;
    private List<OrderRequest> orderRequest;
    private boolean isActive;
    private String userId;
    private boolean isSuccess;
    private String dateTime;
    private String date;

}
