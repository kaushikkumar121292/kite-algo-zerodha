package com.javatechie.spring.mongo.api.model;


import lombok.*;
import lombok.Data;
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserUpdateRequest {
    private String expiry;
    private String quantity;
    private Double maximumRisk;
    private String product;
    private String interval;
}
