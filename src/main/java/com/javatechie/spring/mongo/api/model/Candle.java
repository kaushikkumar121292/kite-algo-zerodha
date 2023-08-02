package com.javatechie.spring.mongo.api.model;

import lombok.*;
import lombok.Data;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Candle {
    private String timestamp;
    private double open;
    private double high;
    private double low;
    private double close;
    private int volume;
}
