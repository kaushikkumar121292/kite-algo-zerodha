package com.javatechie.spring.mongo.api.service;

import com.javatechie.spring.mongo.api.model.OrderRequest;
import com.javatechie.spring.mongo.api.model.TradeDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

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

    @Order(1)
    @Scheduled(fixedDelay = 1000)
    public void terminateTrades() throws IOException {
        Double ltp=ltpService.getLtp();
        TradeDetails activeTrade = tradeDetailsService.getLatestActiveTradeDetails();
        if (activeTrade != null && activeTrade.getPredictedTrend().equals("LONG")) {
            if (ltp >= activeTrade.getTarget()) {
                List<OrderRequest> orders = orderService.getOrders();
                exitTrade(orders);
                activeTrade.setStatus("TARGET");
                tradeDetailsService.saveTradeDetails(activeTrade);
                priceDataService.deleteAllPriceData();
                System.out.println("Long trade hit the target. Trade ID: " + activeTrade.getTradeId());
            } else if (ltp <= activeTrade.getStopLoss()) {
                List<OrderRequest> orders = orderService.getOrders();
                exitTrade(orders);
                activeTrade.setStatus("STOPLOSS");
                tradeDetailsService.saveTradeDetails(activeTrade);
                priceDataService.deleteAllPriceData();
                System.out.println("Long trade hit the stop loss. Trade ID: " + activeTrade.getTradeId());
            }
        } else {
            System.out.println("No active long trades found.");
        }

        if (activeTrade != null && activeTrade.getPredictedTrend().equals("SHORT")) {
            if (ltp <= activeTrade.getTarget()) {
                List<OrderRequest> orders = orderService.getOrders();
                exitTrade(orders);
                activeTrade.setStatus("TARGET");
                tradeDetailsService.saveTradeDetails(activeTrade);
                priceDataService.deleteAllPriceData();
                System.out.println("Short trade hit the target. Trade ID: " + activeTrade.getTradeId());
            } else if (ltp >= activeTrade.getStopLoss()) {
                List<OrderRequest> orders = orderService.getOrders();
                exitTrade(orders);
                activeTrade.setStatus("STOPLOSS");
                tradeDetailsService.saveTradeDetails(activeTrade);
                priceDataService.deleteAllPriceData();
                System.out.println("Short trade hit the stop loss. Trade ID: " + activeTrade.getTradeId());
            }
        } else {
            System.out.println("No active short trades found.");
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

            orderService.placeOrder(orderRequest1);
            orderService.placeOrder(orderRequest2);
            orderService.deleteALLOrders();
        } else {
            System.out.println("Insufficient number of orders to exit the trade.");
        }
    }

}

