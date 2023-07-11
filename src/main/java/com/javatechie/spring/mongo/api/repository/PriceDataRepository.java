package com.javatechie.spring.mongo.api.repository;

import com.javatechie.spring.mongo.api.model.PriceData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceDataRepository extends MongoRepository<PriceData, String> {
    void deleteAll();
}
