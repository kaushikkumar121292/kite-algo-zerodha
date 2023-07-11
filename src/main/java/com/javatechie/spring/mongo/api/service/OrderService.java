package com.javatechie.spring.mongo.api.service;

import com.javatechie.spring.mongo.api.model.UserDetail;
import com.javatechie.spring.mongo.api.repository.OrderRequestRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.javatechie.spring.mongo.api.model.OrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private OrderRequestRepository orderRequestRepository;

    public String placeOrder(OrderRequest orderRequest) throws IOException {
        String url = "https://api.kite.trade/orders/regular";

        List<UserDetail> userDetailList = mongoTemplate.find(
                Query.query(new Criteria()).with(Sort.by(Sort.Direction.DESC, "createdDateTime")).limit(1),
                UserDetail.class
        );
        String encToken = userDetailList.get(0).getEncryptedToken();
        orderRequest.setUserId(userDetailList.get(0).getUserId());

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

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("X-Kite-Version", "3");
        con.setRequestProperty("Authorization", "enctoken " + encToken);
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // Send the POST request
        con.setDoOutput(true);
        try (OutputStream os = con.getOutputStream()) {
            byte[] input = formData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = con.getResponseCode();

        // Read the response
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            return "Order placed successfully";
        }
    }

    @Transactional
    public void saveAll(List<OrderRequest> orderRequests) {
        orderRequestRepository.saveAll(orderRequests);
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
