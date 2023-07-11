package com.javatechie.spring.mongo.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PairData {
    @JsonProperty("Timeframe")
    private String timeframe;

    @JsonProperty("Latest pair")
    private String latestPair;

    @JsonProperty("Latest pair timestamp")
    private String latestPairTimestamp;

    @JsonProperty("Latest pair high")
    private double latestPairHigh;

    @JsonProperty("Latest pair low")
    private double latestPairLow;
}
