package com.javatechie.spring.mongo.api.model;

import lombok.*;
import lombok.Data;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginRequest {
    private String userid;
    private String password;
    private String twofa;
}
