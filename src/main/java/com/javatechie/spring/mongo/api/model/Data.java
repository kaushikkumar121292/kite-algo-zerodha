package com.javatechie.spring.mongo.api.model;

import com.google.gson.annotations.SerializedName;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Data {

    @SerializedName("NSE:NIFTY 50")
    private LtpRequest nseNifty50;
}
