package com.javatechie.spring.mongo.api.model;

import lombok.*;
import lombok.Data;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Trailing {
    private double trailingStopLoss;
    private double trailingTarget;
    private int trailingCount;
}
