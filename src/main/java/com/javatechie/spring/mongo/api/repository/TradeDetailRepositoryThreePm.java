package com.javatechie.spring.mongo.api.repository;

import com.javatechie.spring.mongo.api.model.TradeDetailForThreePm;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TradeDetailRepositoryThreePm extends MongoRepository<TradeDetailForThreePm, String> {
    List<TradeDetailForThreePm> findByIsActive(boolean b);
}