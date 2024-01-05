package com.javatechie.spring.mongo.api.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.javatechie.spring.mongo.api.model.UserDetail;
import com.javatechie.spring.mongo.api.repository.OrderRequestRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.javatechie.spring.mongo.api.model.OrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private OrderRequestRepository orderRequestRepository;

    public String placeOrder(OrderRequest orderRequest, UserDetail user) throws IOException {
        String url = "https://api.kite.trade/orders/regular";

        String encToken = user.getEncryptedToken();
        orderRequest.setUserId(user.getUserId());

        // Set the form data from the request body
        String formData = "exchange=" + orderRequest.getExchange() +
                "&tradingsymbol=" + orderRequest.getTradingSymbol() +
                "&transaction_type=" + orderRequest.getTransactionType() +
                "&order_type=" + orderRequest.getOrderType() +
                "&quantity=" + orderRequest.getQuantity() +
                "&price=" + orderRequest.getPrice() +
                "&product=" + orderRequest.getProduct() +
                "&validity=" + orderRequest.getValidity() +
                "&disclosed_quantity=" + orderRequest.getDisclosedQuantity() +
                "&trigger_price=" + orderRequest.getTriggerPrice() +
                "&squareoff=" + orderRequest.getSquareoff() +
                "&stoploss=" + orderRequest.getStoploss() +
                "&trailing_stoploss=" + orderRequest.getTrailingStoploss() +
                "&user_id=" + orderRequest.getUserId();

        // Create an HTTP client
        HttpClient client = HttpClient.newHttpClient();

        // Build the request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Kite-Version", "3")
                .header("Authorization", "enctoken " + encToken)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        try {
            // Send the request and get the response
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            response.body();

            // Check the response code for success
            if (response.statusCode() == 200) {

                JsonParser jsonParser = new JsonParser();

                JsonObject jsonResponse = jsonParser.parse(response.body().toString()).getAsJsonObject();;

                // Extract the "order_id" from the "data" object
                return jsonResponse.getAsJsonObject("data").get("order_id").getAsString();

            } else {
                // Handle error cases based on the response code and body
                String errorMessage = response.body();
                throw new RuntimeException("Failed to place order. Response code: " + response.statusCode() + ", Error: " + errorMessage);
            }
        } catch (Exception e) {
            // Handle exceptions appropriately
            throw new RuntimeException("Error placing order: " + e.getMessage(), e);
        }
    }

    @Transactional
    public List<OrderRequest> getOrders() {
        return orderRequestRepository.findAll();
    }

    @Transactional
    public void deleteALLOrders() {
        orderRequestRepository.deleteAll();
    }
}
