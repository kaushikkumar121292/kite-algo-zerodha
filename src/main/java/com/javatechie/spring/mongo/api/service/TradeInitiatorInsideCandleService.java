package com.javatechie.spring.mongo.api.service;

import com.javatechie.spring.mongo.api.model.*;
import com.javatechie.spring.mongo.api.repository.UserDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class TradeInitiatorInsideCandleService {

    private static final Logger logger = Logger.getLogger(TradeInitiatorInsideCandleService.class.getName());

    @Autowired
    private  PriceDataInsideCandleService priceDataInsideCandleService;

    @Autowired
    private LtpService ltpService;

    @Autowired
    private TradeDetailsService tradeDetailsService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private  MongoTemplate mongoTemplate;

    @Autowired
    private UserDetailRepository userDetailRepository;

    @Scheduled(fixedDelay = 500) // Execute every 1 seconds
    public void initiateTrade() throws IOException, InterruptedException {
        double highValueMarkedLevel = getHighValue();
        double lowValueMarkedLevel = getLowValue();

        // Code to initiate the trade based on highValue and lowValue
        if (highValueMarkedLevel > 0 && lowValueMarkedLevel > 0) {
            Double ltp = ltpService.getLtp();
            String flag = null;
            if (ltp > highValueMarkedLevel && tradeDetailsService.getLatestActiveTradeDetails() == null) {
                flag = "BULLISH";
                // Initiate long trade (placeOrder long order) for all user
                List<UserDetail> users = getAllUser();
                for (UserDetail user : users) {
                    try {
                        if(user.getMaxTradesPerDay()==user.getTradeCountOfDay()){
                            priceDataInsideCandleService.deleteAllPriceData();
                            throw new RuntimeException("you have reached maximum number of trades allowed per day");
                        }
                        List<OrderRequest> orderRequests=null;
                        if(user.getOptionStrategy().equalsIgnoreCase("BULL_PUT_SPREAD_OR_BEAR_CALL_SPREAD")){
                            orderRequests = getOrderRequestBullPutSpreadOrBearCallSpread(ltp, flag, user);
                            orderService.placeOrder(orderRequests.get(0), user); // Place the buy leg order
                            orderService.placeOrder(orderRequests.get(1), user); // Place the sell leg order
                        }
                        if(user.getOptionStrategy().equalsIgnoreCase("ITM_OPTION_BUYING")){
                            orderRequests = getOrderRequestOptionBuying(ltp, flag, user);
                            //doing plane option buying
                            orderService.placeOrder(orderRequests.get(0),user);
                        }
                        setTargetAndStopLossForLong(highValueMarkedLevel, lowValueMarkedLevel, user, orderRequests);
                        user.setTradeCountOfDay(user.getTradeCountOfDay()+1);
                        userDetailRepository.save(user);
                        logger.log(Level.INFO, "Trade details for user {0} set for LONG.", user.getUserId());
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error while placing order for user : {0}", user.getUserId());
                    }
                }
            } else if (ltp < lowValueMarkedLevel && tradeDetailsService.getLatestActiveTradeDetails() == null) {
                flag = "BEARISH";
                List<UserDetail> users = getAllUser();
                // Initiate short trade(place short order) for all user
                for (UserDetail user : users) {
                    try {
                        if(user.getMaxTradesPerDay()==user.getTradeCountOfDay()){
                            priceDataInsideCandleService.deleteAllPriceData();
                            throw new RuntimeException("you have reached maximum number of trades allowed per day");
                        }
                        List<OrderRequest> orderRequests=null;
                        if(user.getOptionStrategy().equalsIgnoreCase("BULL_PUT_SPREAD_OR_BEAR_CALL_SPREAD")){
                            orderRequests = getOrderRequestBullPutSpreadOrBearCallSpread(ltp, flag, user);
                            orderService.placeOrder(orderRequests.get(0), user); // Place the buy leg order
                            orderService.placeOrder(orderRequests.get(1), user); // Place the sell leg order
                        }
                        if(user.getOptionStrategy().equalsIgnoreCase("ITM_OPTION_BUYING")){
                            orderRequests = getOrderRequestOptionBuying(ltp, flag, user);
                            //doing plane option buying
                            orderService.placeOrder(orderRequests.get(0),user);
                        }
                        setTargetAndSLforShort(highValueMarkedLevel, lowValueMarkedLevel, user, orderRequests);
                        user.setTradeCountOfDay(user.getTradeCountOfDay()+1);
                        userDetailRepository.save(user);
                        logger.log(Level.INFO, "Trade details for user {0} set for SHORT.", user.getUserId());
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error while placing order for user : {0}", user.getUserId());
                    }
                }
            } else {
                logger.log(Level.INFO, "No trade opportunity found.");
            }
        } else {
            logger.log(Level.INFO, "Level not yet marked by user.");
        }
    }

    private List<OrderRequest> getOrderRequestOptionBuying(Double spotPrice, String flag, UserDetail user) {
        int ATM = (int) (Math.round(spotPrice / 50) * 50);
        List<OrderRequest> orderRequest = null;
        if (flag.equalsIgnoreCase("BULLISH")) {
            OrderRequest callBuy = new OrderRequest();
            callBuy.setDisclosedQuantity("0");
            callBuy.setExchange("NFO");
            callBuy.setOrderType("MARKET");
            callBuy.setPrice("0");
            callBuy.setProduct(user.getProduct());
            callBuy.setQuantity(user.getQuantity());
            callBuy.setSquareoff("0");
            callBuy.setStoploss("0");
            callBuy.setTrailingStoploss("0");
            callBuy.setTriggerPrice("0");
            callBuy.setUserId(null);
            callBuy.setValidity("DAY");
            callBuy.setTransactionType("BUY");
            callBuy.setTradingSymbol("NIFTY" + user.getExpiry() + (ATM - 200) + "CE");
            orderRequest = new ArrayList<>();
            orderRequest.add(callBuy);

        }else if (flag.equalsIgnoreCase("BEARISH")) {

            OrderRequest putBuy = new OrderRequest();
            putBuy.setDisclosedQuantity("0");
            putBuy.setExchange("NFO");
            putBuy.setOrderType("MARKET");
            putBuy.setPrice("0");
            putBuy.setProduct(user.getProduct());
            putBuy.setQuantity(user.getQuantity());
            putBuy.setSquareoff("0");
            putBuy.setStoploss("0");
            putBuy.setTrailingStoploss("0");
            putBuy.setTriggerPrice("0");
            putBuy.setUserId(null);
            putBuy.setValidity("DAY");
            putBuy.setTransactionType("BUY");
            putBuy.setTradingSymbol("NIFTY" + user.getExpiry() + (ATM + 150) + "PE");
            orderRequest = new ArrayList<>();
            orderRequest.add(putBuy);
        }

        return orderRequest;
    }

    private List<OrderRequest> getOrderRequestBullPutSpreadOrBearCallSpread(Double spotPrice, String flag, UserDetail user) {
        int ATM = (int) (Math.round(spotPrice / 50) * 50);
        List<OrderRequest> orderRequests = null;
        if (flag.equalsIgnoreCase("BULLISH")) {
            OrderRequest leg1 = new OrderRequest();
            leg1.setDisclosedQuantity("0");
            leg1.setExchange("NFO");
            leg1.setOrderType("MARKET");
            leg1.setPrice("0");
            leg1.setProduct(user.getProduct());
            leg1.setQuantity(user.getQuantity());
            leg1.setSquareoff("0");
            leg1.setStoploss("0");
            leg1.setTrailingStoploss("0");
            leg1.setTriggerPrice("0");
            leg1.setUserId(null);
            leg1.setValidity("DAY");
            leg1.setTransactionType("BUY");
            leg1.setTradingSymbol("NIFTY" + user.getExpiry() + (ATM - 150) + "PE");

            OrderRequest leg2 = new OrderRequest();
            leg2.setDisclosedQuantity("0");
            leg2.setExchange("NFO");
            leg2.setOrderType("MARKET");
            leg2.setPrice("0");
            leg2.setProduct(user.getProduct());
            leg2.setQuantity(user.getQuantity());
            leg2.setSquareoff("0");
            leg2.setStoploss("0");
            leg2.setTrailingStoploss("0");
            leg2.setTriggerPrice("0");
            leg2.setUserId(null);
            leg2.setValidity("DAY");
            leg2.setTransactionType("SELL");
            leg2.setTradingSymbol("NIFTY" + user.getExpiry() + (ATM+50) + "PE");

            orderRequests = new ArrayList<>();
            orderRequests.add(leg1);
            orderRequests.add(leg2);
        } else if (flag.equalsIgnoreCase("BEARISH")) {
            OrderRequest leg1 = new OrderRequest();
            leg1.setDisclosedQuantity("0");
            leg1.setExchange("NFO");
            leg1.setOrderType("MARKET");
            leg1.setPrice("0");
            leg1.setProduct(user.getProduct());
            leg1.setQuantity(user.getQuantity());
            leg1.setSquareoff("0");
            leg1.setStoploss("0");
            leg1.setTrailingStoploss("0");
            leg1.setTriggerPrice("0");
            leg1.setUserId(null);
            leg1.setValidity("DAY");
            leg1.setTransactionType("BUY");
            leg1.setTradingSymbol("NIFTY" + user.getExpiry() + (ATM + 250) + "CE");

            OrderRequest leg2 = new OrderRequest();
            leg2.setDisclosedQuantity("0");
            leg2.setExchange("NFO");
            leg2.setOrderType("MARKET");
            leg2.setPrice("0");
            leg2.setProduct(user.getProduct());
            leg2.setQuantity(user.getQuantity());
            leg2.setSquareoff("0");
            leg2.setStoploss("0");
            leg2.setTrailingStoploss("0");
            leg2.setTriggerPrice("0");
            leg2.setUserId(null);
            leg2.setValidity("DAY");
            leg2.setTransactionType("SELL");
            leg2.setTradingSymbol("NIFTY" + user.getExpiry() + (ATM-50) + "CE");

            orderRequests = new ArrayList<>();
            orderRequests.add(leg1);
            orderRequests.add(leg2);
        }

        return orderRequests;
    }

    private void setTargetAndSLforShort(Double highValueMarkedLevel,Double lowValueMarkedLevel, UserDetail user, List<OrderRequest> orderRequests) throws IOException {
        Double entry = ltpService.getLtp();
        TradeDetails tradeDetails = new TradeDetails();
        tradeDetails.setEntry(entry);
        Double defaultRisk=highValueMarkedLevel-lowValueMarkedLevel;
        Double risk=defaultRisk;
        if(defaultRisk>user.getMaximumRisk()){
            risk=user.getMaximumRisk();
        }
        Double reward=risk*3;
        tradeDetails.setTarget(entry - reward);
        tradeDetails.setStopLoss(entry + risk);
        tradeDetails.setStatus("ACTIVE");
        tradeDetails.setPredictedTrend("SHORT");
        ZoneId zoneId = ZoneId.of("Asia/Kolkata");
        tradeDetails.setDateTime(LocalDateTime.now(zoneId).toString());
        tradeDetails.setUserId(user.getUserId());
        tradeDetails.setOrderRequests(orderRequests);
        tradeDetails.setRisk(risk);
        tradeDetails.setTrailingCount(0);
        tradeDetailsService.saveTradeDetails(tradeDetails);
    }

    private void setTargetAndStopLossForLong(Double highValueMarkedLevel,Double lowValueMarkedLevel, UserDetail user, List<OrderRequest> orderRequests)  throws IOException {
        Double entry = ltpService.getLtp();
        TradeDetails tradeDetails = new TradeDetails();
        tradeDetails.setEntry(entry);
        Double defaultRisk=highValueMarkedLevel-lowValueMarkedLevel;
        Double risk=defaultRisk;
        if(user.getMaximumRisk()!=0 && defaultRisk>user.getMaximumRisk() ){
            risk=user.getMaximumRisk();
        }
        Double reward=risk*3;
        tradeDetails.setTarget(entry + reward);
        tradeDetails.setStopLoss(entry - risk);
        tradeDetails.setStatus("ACTIVE");
        tradeDetails.setPredictedTrend("LONG");
        ZoneId zoneId = ZoneId.of("Asia/Kolkata");
        tradeDetails.setDateTime(LocalDateTime.now(zoneId).toString());
        tradeDetails.setUserId(user.getUserId());
        tradeDetails.setOrderRequests(orderRequests);
        tradeDetails.setRisk(risk);
        tradeDetails.setTrailingCount(0);
        tradeDetailsService.saveTradeDetails(tradeDetails);
    }

    public Double getHighValue() {
        ResponseEntity<PriceDataInsideCandle> response = priceDataInsideCandleService.getLatestPriceData();
        if (response.getStatusCode().is2xxSuccessful()) {
            PriceDataInsideCandle latestPriceData = response.getBody();
            if (latestPriceData != null) {
                return latestPriceData.getHigh();
            }
        }
        // Return a default value or handle the error case
        return null;
    }

    public Double getLowValue() {
        ResponseEntity<PriceDataInsideCandle> response = priceDataInsideCandleService.getLatestPriceData();
        if (response.getStatusCode().is2xxSuccessful()) {
            PriceDataInsideCandle latestPriceData = response.getBody();
            if (latestPriceData != null) {
                return latestPriceData.getLow();
            }
        }
        // Return a default value or handle the error case
        return null;
    }


    private List<UserDetail> getAllUser() {
        List<UserDetail> userDetailList = userDetailRepository.findAll();
        if (!userDetailList.isEmpty()) {
            return userDetailList;
        } else {
            return null;
        }
    }
}
