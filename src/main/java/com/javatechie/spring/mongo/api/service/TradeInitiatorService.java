package com.javatechie.spring.mongo.api.service;

import com.javatechie.spring.mongo.api.model.OrderRequest;
import com.javatechie.spring.mongo.api.model.PriceData;
import com.javatechie.spring.mongo.api.model.TradeDetails;
import com.javatechie.spring.mongo.api.model.UserDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;

@Service
public class TradeInitiatorService {

    @Autowired
    private PriceDataService priceDataService;

    @Autowired
    private LtpService ltpService;

    @Autowired
    private TradeDetailsService tradeDetailsService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private  MongoTemplate mongoTemplate;

    @Order(3)
    @Scheduled(fixedDelay = 1000) // Execute every 1 seconds
    public void initiateTrade() throws IOException, InterruptedException {
        double highValueMarkedLevel = getHighValue();
        double lowValueMarkedLevel = getLowValue();

        // Code to initiate the trade based on highValue and lowValue
        if (highValueMarkedLevel > 0 && lowValueMarkedLevel > 0) {
            Double ltp = ltpService.getLtp();
            String flag = null;
            if (ltp > highValueMarkedLevel && tradeDetailsService.getLatestActiveTradeDetails() == null) {
                flag = "BULLISH";
                // Initiate long trade (placeOrder long order)
                List<OrderRequest> orderRequests = getOrderRequest(ltp, flag);
                orderService.placeOrder(orderRequests.get(0)); // Place the buy leg order
                orderService.placeOrder(orderRequests.get(1)); // Place the sell leg order
                setTargetAndStopLossForLong(highValueMarkedLevel,lowValueMarkedLevel);
                orderService.saveAll(orderRequests);
            } else if (ltp < lowValueMarkedLevel && tradeDetailsService.getLatestActiveTradeDetails() == null) {
                flag = "BEARISH";
                // Initiate short trade(place short order)
                List<OrderRequest> orderRequests = getOrderRequest(ltp, flag);
                orderService.placeOrder(orderRequests.get(0)); // Place the buy leg order
                orderService.placeOrder(orderRequests.get(1)); // Place the sell leg order
                setTargetAndSLforShort(highValueMarkedLevel,lowValueMarkedLevel);
                orderService.saveAll(orderRequests);
            } else {
                System.out.println("No trade opportunity found.");
            }
        } else {
            System.out.println("Level not yet marked by user.");
        }
    }


    private List<OrderRequest> getOrderRequest(Double spotPrice, String flag) {
        int ATM = (int) (Math.round(spotPrice / 50) * 50);
        List<OrderRequest> orderRequests = null;
        if (flag.equalsIgnoreCase("BULLISH")) {
            OrderRequest leg1 = new OrderRequest();
            leg1.setDisclosedQuantity("0");
            leg1.setExchange("NFO");
            leg1.setOrderType("MARKET");
            leg1.setPrice("0");
            leg1.setProduct(getLatestCreatedUser().getProduct());
            leg1.setQuantity(getLatestCreatedUser().getQuantity());
            leg1.setSquareoff("0");
            leg1.setStoploss("0");
            leg1.setTrailingStoploss("0");
            leg1.setTriggerPrice("0");
            leg1.setUserId(null);
            leg1.setValidity("DAY");
            leg1.setTransactionType("BUY");
            leg1.setTradingSymbol("NIFTY" + getLatestCreatedUser().getExpiry() + (ATM - 200) + "PE");

            OrderRequest leg2 = new OrderRequest();
            leg2.setDisclosedQuantity("0");
            leg2.setExchange("NFO");
            leg2.setOrderType("MARKET");
            leg2.setPrice("0");
            leg2.setProduct(getLatestCreatedUser().getProduct());
            leg2.setQuantity(getLatestCreatedUser().getQuantity());
            leg2.setSquareoff("0");
            leg2.setStoploss("0");
            leg2.setTrailingStoploss("0");
            leg2.setTriggerPrice("0");
            leg2.setUserId(null);
            leg2.setValidity("DAY");
            leg2.setTransactionType("SELL");
            leg2.setTradingSymbol("NIFTY" + getLatestCreatedUser().getExpiry() + ATM + "PE");

            orderRequests = new ArrayList<>();
            orderRequests.add(leg1);
            orderRequests.add(leg2);
        } else if (flag.equalsIgnoreCase("BEARISH")) {
            OrderRequest leg1 = new OrderRequest();
            leg1.setDisclosedQuantity("0");
            leg1.setExchange("NFO");
            leg1.setOrderType("MARKET");
            leg1.setPrice("0");
            leg1.setProduct(getLatestCreatedUser().getProduct());
            leg1.setQuantity(getLatestCreatedUser().getQuantity());
            leg1.setSquareoff("0");
            leg1.setStoploss("0");
            leg1.setTrailingStoploss("0");
            leg1.setTriggerPrice("0");
            leg1.setUserId(null);
            leg1.setValidity("DAY");
            leg1.setTransactionType("BUY");
            leg1.setTradingSymbol("NIFTY" + getLatestCreatedUser().getExpiry() + (ATM + 200) + "CE");

            OrderRequest leg2 = new OrderRequest();
            leg2.setDisclosedQuantity("0");
            leg2.setExchange("NFO");
            leg2.setOrderType("MARKET");
            leg2.setPrice("0");
            leg2.setProduct(getLatestCreatedUser().getProduct());
            leg2.setQuantity(getLatestCreatedUser().getQuantity());
            leg2.setSquareoff("0");
            leg2.setStoploss("0");
            leg2.setTrailingStoploss("0");
            leg2.setTriggerPrice("0");
            leg2.setUserId(null);
            leg2.setValidity("DAY");
            leg2.setTransactionType("SELL");
            leg2.setTradingSymbol("NIFTY" + getLatestCreatedUser().getExpiry() + ATM + "CE");

            orderRequests = new ArrayList<>();
            orderRequests.add(leg1);
            orderRequests.add(leg2);
        }

        return orderRequests;
    }


    private void setTargetAndSLforShort(Double highValueMarkedLevel,Double lowValueMarkedLevel) throws IOException {
        Double entry = ltpService.getLtp();
        TradeDetails tradeDetails = new TradeDetails();
        tradeDetails.setEntry(entry);
        Double defaultRisk=highValueMarkedLevel-lowValueMarkedLevel;
        Double risk=defaultRisk;
        if(defaultRisk>getLatestCreatedUser().getMaximumRisk()){
            risk=getLatestCreatedUser().getMaximumRisk();
        }
        Double reward=risk*3;
        tradeDetails.setTarget(entry - reward);
        tradeDetails.setStopLoss(entry + risk);
        tradeDetails.setStatus("ACTIVE");
        tradeDetails.setPredictedTrend("SHORT");
        ZoneId zoneId = ZoneId.of("Asia/Kolkata");
        tradeDetails.setDateTime(LocalTime.now(zoneId).toString());
        tradeDetailsService.saveTradeDetails(tradeDetails);
    }

    private void setTargetAndStopLossForLong(Double highValueMarkedLevel,Double lowValueMarkedLevel)  throws IOException {
        Double entry = ltpService.getLtp();
        TradeDetails tradeDetails = new TradeDetails();
        tradeDetails.setEntry(entry);
        Double defaultRisk=highValueMarkedLevel-lowValueMarkedLevel;
        Double risk=defaultRisk;
        if(defaultRisk>getLatestCreatedUser().getMaximumRisk()){
            risk=getLatestCreatedUser().getMaximumRisk();
        }
        Double reward=risk*3;
        tradeDetails.setTarget(entry + reward);
        tradeDetails.setStopLoss(entry - risk);
        tradeDetails.setStatus("ACTIVE");
        tradeDetails.setPredictedTrend("LONG");
        ZoneId zoneId = ZoneId.of("Asia/Kolkata");
        tradeDetails.setDateTime(LocalTime.now(zoneId).toString());
        tradeDetailsService.saveTradeDetails(tradeDetails);
    }

    public Double getHighValue() {
        ResponseEntity<PriceData> response = priceDataService.getLatestPriceData();
        if (response.getStatusCode().is2xxSuccessful()) {
            PriceData latestPriceData = response.getBody();
            if (latestPriceData != null) {
                return latestPriceData.getHigh();
            }
        }
        // Return a default value or handle the error case
        return null;
    }

    public Double getLowValue() {
        ResponseEntity<PriceData> response = priceDataService.getLatestPriceData();
        if (response.getStatusCode().is2xxSuccessful()) {
            PriceData latestPriceData = response.getBody();
            if (latestPriceData != null) {
                return latestPriceData.getLow();
            }
        }
        // Return a default value or handle the error case
        return null;
    }


    private UserDetail getLatestCreatedUser() {
        List<UserDetail> userDetailList = mongoTemplate.find(
                Query.query(new Criteria()).with(Sort.by(Sort.Direction.DESC, "createdDateTime")).limit(1),
                UserDetail.class
        );
        if (!userDetailList.isEmpty()) {
            return userDetailList.get(0);
        } else {
            return null;
        }
    }
}
