package com.javatechie.spring.mongo.api.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.javatechie.spring.mongo.api.model.TradeDetails;

public interface TradeDetailsRepository extends MongoRepository<TradeDetails, String>{

}
