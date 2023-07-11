package com.javatechie.spring.mongo.api.model;

import lombok.Data;

import lombok.Data;

@Data
public class HistoricalDataRequest {
    private String instrumentToken;
    private String from;
    private String to;
    private String interval;
}

