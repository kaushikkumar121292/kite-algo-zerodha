package com.javatechie.spring.mongo.api.repository;

import com.javatechie.spring.mongo.api.model.PriceDataInsideCandle;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceDataInsideCandleRepository extends MongoRepository<PriceDataInsideCandle, String> {
    void deleteAll();
}
