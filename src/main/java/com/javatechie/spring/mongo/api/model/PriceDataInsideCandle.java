package com.javatechie.spring.mongo.api.model;

import lombok.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "PRICE_DATA_INSIDE_CANDLE")
public class PriceDataInsideCandle {
    @Id
    private String id;
    private double high;
    private double low;
    private String status;
}
