package com.javatechie.spring.mongo.api.repository;

import com.javatechie.spring.mongo.api.model.TradeDetailForThreePm;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface TradeDetailRepositoryThreePm extends MongoRepository<TradeDetailForThreePm, String> {
    List<TradeDetailForThreePm> findByIsActive(boolean b);

    @Query("{'date': ?0, 'isSuccess': ?1, 'isActive': ?2, 'userId': ?3}")
    List<TradeDetailForThreePm> findByDateAndIsSuccessAndIsActiveAndUserId(
            String date, boolean isSuccess, boolean isActive, String userId);

}