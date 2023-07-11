package com.javatechie.spring.mongo.api.model;

import com.google.gson.annotations.SerializedName;
import lombok.*;
import lombok.Data;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LtpRequest {

    @SerializedName("instrument_token")
    private Integer instrumentToken;

    @SerializedName("last_price")
    private Double lastprice;

}
