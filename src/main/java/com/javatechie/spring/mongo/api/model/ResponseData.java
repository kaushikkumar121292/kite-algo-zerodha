package com.javatechie.spring.mongo.api.model;

import lombok.Data;

import java.util.List;

@Data
public class ResponseData {

    private List<Candle> candles;
}


