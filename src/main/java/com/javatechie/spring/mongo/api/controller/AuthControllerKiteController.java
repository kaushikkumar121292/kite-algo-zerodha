package com.javatechie.spring.mongo.api.controller;

import com.javatechie.spring.mongo.api.model.LoginRequest;
import io.swagger.annotations.Api;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.javatechie.spring.mongo.api.model.UserDetail;
import com.javatechie.spring.mongo.api.repository.UserDetailRepository;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.var;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



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

    @PostMapping("/create-user-by-login")
    public ResponseEntity<UserDetail> createUserByLogin(@RequestBody LoginRequest loginRequest) {
        UserDetail user = new UserDetail();
        user.setEncryptedToken(login(loginRequest));
        user.setCreatedDateTime(LocalDateTime.now());
        user.setUserId(loginRequest.getUserid());
        user.setPassword(loginRequest.getPassword());
        user.setTwofa(loginRequest.getTwofa());

        UserDetail createdUser = userDetailRepository.save(user);

        // Set the URI for the newly created user in the response headers
        String uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdUser.getUserId()) // Assuming you have a getId() method in UserDetail class
                .toUriString();

        return ResponseEntity.created(URI.create(uri)).body(createdUser);
    }

    @PostMapping("/create-user-by-token")
    public UserDetail createUserByToken(@RequestBody UserDetail userDetail) {
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



    public String login(LoginRequest loginRequest) {
        try {
            String userid = loginRequest.getUserid();
            String password = loginRequest.getPassword();
            String twofa = loginRequest.getTwofa();

            // Create a CookieManager to handle cookies
            CookieHandler.setDefault(new CookieManager());

            // Step 1: Login
            String loginUrl = "https://kite.zerodha.com/api/login";
            String loginData = "user_id=" + URLEncoder.encode(userid, "UTF-8") +
                    "&password=" + URLEncoder.encode(password, "UTF-8");

            String requestId = "";
            String responseUserId = "";

            HttpURLConnection loginConnection = (HttpURLConnection) new URL(loginUrl).openConnection();
            loginConnection.setRequestMethod("POST");
            loginConnection.setDoOutput(true);

            try (var outputStream = loginConnection.getOutputStream()) {
                outputStream.write(loginData.getBytes("UTF-8"));
            }

            BufferedReader loginReader = new BufferedReader(new InputStreamReader(loginConnection.getInputStream()));
            StringBuilder loginResponse = new StringBuilder();
            String loginLine;
            while ((loginLine = loginReader.readLine()) != null) {
                loginResponse.append(loginLine);
            }
            loginReader.close();
            loginConnection.disconnect();

            JsonParser loginParser = new JsonParser();
            JsonObject loginResponseJson = loginParser.parse(loginResponse.toString()).getAsJsonObject();
            requestId = loginResponseJson.get("data").getAsJsonObject().get("request_id").getAsString();
            responseUserId = loginResponseJson.get("data").getAsJsonObject().get("user_id").getAsString();

            // Step 2: Two-factor authentication
            String twofaUrl = "https://kite.zerodha.com/api/twofa";
            String twofaData = "request_id=" + URLEncoder.encode(requestId, "UTF-8") +
                    "&twofa_value=" + URLEncoder.encode(twofa, "UTF-8") +
                    "&user_id=" + URLEncoder.encode(responseUserId, "UTF-8");

            HttpURLConnection twofaConnection = (HttpURLConnection) new URL(twofaUrl).openConnection();
            twofaConnection.setRequestMethod("POST");
            twofaConnection.setDoOutput(true);

            try (var outputStream = twofaConnection.getOutputStream()) {
                outputStream.write(twofaData.getBytes("UTF-8"));
            }

            BufferedReader twofaReader = new BufferedReader(new InputStreamReader(twofaConnection.getInputStream()));
            StringBuilder twofaResponse = new StringBuilder();
            String twofaLine;
            while ((twofaLine = twofaReader.readLine()) != null) {
                twofaResponse.append(twofaLine);
            }
            twofaReader.close();
            twofaConnection.disconnect();

            String cookieHeader = twofaConnection.getHeaderField("Set-Cookie");
            String enctoken = extractEnctokenFromCookie(cookieHeader);
            if (enctoken != null) {
                return enctoken;
            } else {
                throw new Exception("Enter valid details !!!!");
            }

            // Continue with the remaining logic...
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    private static String extractEnctokenFromCookie(String cookieHeader) {
        if (cookieHeader != null) {
            Pattern pattern = Pattern.compile("enctoken=(.*?);");
            Matcher matcher = pattern.matcher(cookieHeader);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }


}
