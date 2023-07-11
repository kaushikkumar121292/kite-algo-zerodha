package com.javatechie.spring.mongo.api.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LtpQuotes {

    @SerializedName("status")
    private String status;
    @SerializedName("data")
    private Data data;

}
