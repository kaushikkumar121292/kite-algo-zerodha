package com.javatechie.spring.mongo.api.controller;

import com.javatechie.spring.mongo.api.service.ExtractHighLowFromJSONObjectService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import com.google.gson.Gson;
import com.javatechie.spring.mongo.api.model.*;
import com.javatechie.spring.mongo.api.repository.PriceDataRepository;
import com.javatechie.spring.mongo.api.repository.UserDetailRepository;
import com.javatechie.spring.mongo.api.service.HistoricalDataService;
import com.javatechie.spring.mongo.api.service.OrderService;
import com.javatechie.spring.mongo.api.service.TradeInitiatorSchedulerService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.javatechie.spring.mongo.api.repository.TradeDetailsRepository;

@RestController
@RequestMapping("/trade")
@Api(value = "Trade Detail Service")
public class TradeDetailController {

    private final Gson gson = new Gson();

    private final String rootUrl = "https://api.kite.trade";
    private Map<String, String> headers;

    @Autowired
    private TradeDetailsRepository repository;

    @Autowired
    private UserDetailRepository userDetailRepository;

    @Autowired
    private HistoricalDataService historicalDataService;

    @Autowired
    private PriceDataRepository priceDataRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TradeInitiatorSchedulerService tradeInitiatorService;

    @Autowired
    private  ExtractHighLowFromJSONObjectService extractService;


    @GetMapping("/ltp")
    public ResponseEntity<LtpQuotes> getLtpEndpoint() throws IOException {
        String url = rootUrl + "/quote/ltp?i=NSE:NIFTY+50";
        headers = getHeadersWithEnctoken();

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String json = response.toString();

            LtpQuotes ltpQuotes = gson.fromJson(json, LtpQuotes.class);
            return ResponseEntity.ok(ltpQuotes);
        } else {
            // Handle error response
            return ResponseEntity.status(responseCode).build();
        }
    }

    @PostMapping("/historyData")
    public ResponseEntity<String> getHistoricalData(@RequestBody HistoricalDataRequest historicalDataRequest) {
        try {
            String historicalData = historicalDataService.getHistoryDataOfInstrument(
                    historicalDataRequest.getInstrumentToken(),
                    historicalDataRequest.getFrom(),
                    historicalDataRequest.getTo(),
                    historicalDataRequest.getInterval()
            );
            if (historicalData != null) {
                return ResponseEntity.ok(historicalData);
            } else {
                // Handle the case when historical data is not available
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            // Handle the exception and return an appropriate response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("/markLevel")
    public ResponseEntity<String> savePriceData(@RequestBody List<PriceDataTraffic> priceDataList) {
        try {
            priceDataRepository.saveAll(priceDataList);
            return ResponseEntity.ok("Price data saved successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save price data");
        }
    }

    @GetMapping("/get-latest-level")
    public ResponseEntity<PriceDataTraffic> getLatestPriceData() {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(0, 1, sort);
        Page<PriceDataTraffic> page = priceDataRepository.findAll(pageable);
        if (page.hasContent()) {
            PriceDataTraffic latestPriceData = page.getContent().get(0);
            return ResponseEntity.ok(latestPriceData);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/delete-price-data")
    public ResponseEntity<String> deleteAllPriceData() {
        try {
            priceDataRepository.deleteAll();
            return ResponseEntity.ok("All PriceData documents deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete PriceData documents");
        }
    }

    @PostMapping("/place-order")
    public ResponseEntity<String> placeOrder(@RequestBody OrderRequest orderRequest) {
        try {
            String response = orderService.placeOrder(orderRequest, userDetailRepository.findById(orderRequest.getUserId()).get());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>("Failed to place order: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/fetch-order")
    public ResponseEntity<List<OrderRequest>> fetchOrder() {
        List<OrderRequest> orders = orderService.getOrders();
        return new ResponseEntity<>(orders,HttpStatus.OK);
    }

    @DeleteMapping("/delete-order")
    public ResponseEntity<String> deleteOrder() {
        orderService.deleteALLOrders();
        return new ResponseEntity<>("Order deleted",HttpStatus.OK);
    }


    @PostMapping("/initiate-trade")
    public ResponseEntity<String> initiateTrade() throws IOException, InterruptedException {
        tradeInitiatorService.initiateTrade();
        return new ResponseEntity<>("initiated successfully",HttpStatus.OK);
    }




    private Map<String, String> getHeadersWithEnctoken() {
        Map<String, String> headers = new HashMap<>();
        List<UserDetail> userDetailList = mongoTemplate.find(
                Query.query(new Criteria()).with(Sort.by(Sort.Direction.DESC, "createdDateTime")).limit(1),
                UserDetail.class
        );
        headers.put("Authorization", "enctoken " + userDetailList.get(0).getEncryptedToken());
        return headers;
    }


    @GetMapping("/highlow")
    public List<Double> getHighLowList(@RequestParam("jsonResponse") String jsonResponse) {
        return extractService.getHighLowList(jsonResponse);
    }

}
