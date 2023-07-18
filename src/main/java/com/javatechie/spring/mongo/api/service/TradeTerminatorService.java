package com.javatechie.spring.mongo.api.service;

import com.javatechie.spring.mongo.api.model.OrderRequest;
import com.javatechie.spring.mongo.api.model.TradeDetails;
import com.javatechie.spring.mongo.api.model.UserDetail;
import com.javatechie.spring.mongo.api.repository.UserDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class TradeTerminatorService {

    @Autowired
    private TradeDetailsService tradeDetailsService;

    @Autowired
    private LtpService ltpService;

    @Autowired
    private PriceDataService priceDataService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserDetailRepository userDetailRepository;


    @Order(1)
    @Scheduled(fixedDelay = 1000)
    public void terminateTrades() throws IOException {
        Double ltp=ltpService.getLtp();
        List<TradeDetails> activeTrades = tradeDetailsService.getLatestActiveTradeDetails();
        for (TradeDetails activeTrade :activeTrades) {
            if (activeTrade != null && activeTrade.getPredictedTrend().equals("LONG")) {
                if (ltp >= activeTrade.getTarget()) {
                    try {
                        List<OrderRequest> orders = activeTrade.getOrderRequests();
                        exitTrade(orders);
                        activeTrade.setStatus("TARGET");
                        tradeDetailsService.saveTradeDetails(activeTrade);
                        System.out.println("Long trade hit the target for User ID: " + activeTrade.getUserId());
                    } catch (IOException e) {
                        throw new RuntimeException("Error while exiting the trade for User Id: " +activeTrade.getUserId());
                    }
                } else if (ltp <= activeTrade.getStopLoss()) {
                    try {
                        List<OrderRequest> orders = activeTrade.getOrderRequests();
                        exitTrade(orders);
                        activeTrade.setStatus("STOPLOSS");
                        tradeDetailsService.saveTradeDetails(activeTrade);
                        System.out.println("Long trade hit the stop loss. User ID: " + activeTrade.getUserId());
                    } catch (IOException e) {
                        throw new RuntimeException("Error while exiting the trade for User Id: " +activeTrade.getUserId());
                    }
                }
            } else {
                System.out.println("No active long trades found.");
            }

            if (activeTrade != null && activeTrade.getPredictedTrend().equals("SHORT")) {
                if (ltp <= activeTrade.getTarget()) {
                    try {
                        List<OrderRequest> orders = activeTrade.getOrderRequests();
                        exitTrade(orders);
                        activeTrade.setStatus("TARGET");
                        tradeDetailsService.saveTradeDetails(activeTrade);
                        System.out.println("Short trade hit the target. User ID: " + activeTrade.getUserId());
                    } catch (IOException e) {
                        throw new RuntimeException("Error while exiting the trade for User Id: " +activeTrade.getUserId());
                    }
                } else if (ltp >= activeTrade.getStopLoss()) {
                    try {
                        List<OrderRequest> orders = activeTrade.getOrderRequests();
                        exitTrade(orders);
                        activeTrade.setStatus("STOPLOSS");
                        tradeDetailsService.saveTradeDetails(activeTrade);
                        System.out.println("Short trade hit the stop loss. User ID: " + activeTrade.getUserId());
                    } catch (IOException e) {
                        throw new RuntimeException("Error while exiting the trade for User Id: " +activeTrade.getUserId());
                    }
                }
            } else {
                System.out.println("No active short trades found.");
            }
        }
    }

    private void exitTrade(List<OrderRequest> orders) throws IOException {
        if (orders.size() >= 2) {
            OrderRequest orderRequest1 = orders.get(0);
            OrderRequest orderRequest2 = orders.get(1);

            if (orderRequest1.getTransactionType().equalsIgnoreCase("BUY")) {
                orderRequest1.setTransactionType("SELL");
            } else if (orderRequest1.getTransactionType().equalsIgnoreCase("SELL")) {
                orderRequest1.setTransactionType("BUY");
            }

            if (orderRequest2.getTransactionType().equalsIgnoreCase("BUY")) {
                orderRequest2.setTransactionType("SELL");
            } else if (orderRequest2.getTransactionType().equalsIgnoreCase("SELL")) {
                orderRequest2.setTransactionType("BUY");
            }


            UserDetail user = userDetailRepository.findById(orderRequest1.getUserId()).get();

            orderService.placeOrder(orderRequest1, user);
            orderService.placeOrder(orderRequest2, user);
        } else {
            System.out.println("Insufficient number of orders to exit the trade.");
        }
    }

}

