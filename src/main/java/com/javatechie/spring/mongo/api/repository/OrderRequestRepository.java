package com.javatechie.spring.mongo.api.repository;

import com.javatechie.spring.mongo.api.model.OrderRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface OrderRequestRepository extends MongoRepository<OrderRequest, String> {

}
