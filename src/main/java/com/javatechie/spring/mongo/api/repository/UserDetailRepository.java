package com.javatechie.spring.mongo.api.repository;

import com.javatechie.spring.mongo.api.model.UserDetail;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserDetailRepository extends MongoRepository<UserDetail, String> {


}
