package com.javatechie.spring.mongo.api.model;

import lombok.Data;

@Data
public class Candle {
    private String timestamp;
    private double open;
    private double high;
    private double low;
    private double close;
    private int volume;
}
