package com.javatechie.spring.mongo.api.model;

import com.google.gson.annotations.SerializedName;
import lombok.*;
import lombok.Data;

import java.util.ArrayList;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RowData {

    @SerializedName("candles")
    public ArrayList<ArrayList<Object>> candles;
}
