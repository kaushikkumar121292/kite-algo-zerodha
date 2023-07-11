package com.javatechie.spring.mongo.api.controller;

import io.swagger.annotations.Api;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.javatechie.spring.mongo.api.model.UserDetail;
import com.javatechie.spring.mongo.api.repository.UserDetailRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/tradeKite")
@Api(value = "AuthKite")
public class AuthControllerKiteController {

    private final UserDetailRepository userDetailRepository;
    private final MongoTemplate mongoTemplate;

    public AuthControllerKiteController(UserDetailRepository userDetailRepository, MongoTemplate mongoTemplate) {
        this.userDetailRepository = userDetailRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @PostMapping("/users")
    public UserDetail createUserDetail(@RequestBody UserDetail userDetail) {
        userDetail.setCreatedDateTime(LocalDateTime.now());
        return userDetailRepository.save(userDetail);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserDetail> getUserDetail(@PathVariable String userId) {
        Optional<UserDetail> optionalUserDetail = userDetailRepository.findById(userId);
        return optionalUserDetail.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/latest-added-user")
    public ResponseEntity<UserDetail> getLatestCreatedUser() {
        List<UserDetail> userDetailList = mongoTemplate.find(
                Query.query(new Criteria()).with(Sort.by(Sort.Direction.DESC, "createdDateTime")).limit(1),
                UserDetail.class
        );
        if (!userDetailList.isEmpty()) {
            return ResponseEntity.ok(userDetailList.get(0));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
