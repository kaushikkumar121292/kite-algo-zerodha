package com.javatechie.spring.mongo.api.model;

import lombok.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "USER_DETAIL")
public class UserDetail {

    @Id
    private String userId;
    private String password;
    private String twofa;
    private String encryptedToken;
    private LocalDateTime createdDateTime;
    private String expiry;
    private String quantity;
    private Double maximumRisk;
    private String product;
    private String interval;
    private String strategy;
}
