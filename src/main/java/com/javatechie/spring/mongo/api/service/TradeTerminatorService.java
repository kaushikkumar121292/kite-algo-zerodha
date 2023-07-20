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
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class TradeTerminatorService {

    private static final Logger logger = Logger.getLogger(TradeTerminatorService.class.getName());

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
                        logger.log(Level.INFO, "Long trade hit the target for User ID: {0}", activeTrade.getUserId());
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error while exiting the trade for User Id: {0}", activeTrade.getUserId());
                    }
                } else if (ltp <= activeTrade.getStopLoss()) {
                    try {
                        List<OrderRequest> orders = activeTrade.getOrderRequests();
                        exitTrade(orders);
                        activeTrade.setStatus("STOPLOSS");
                        tradeDetailsService.saveTradeDetails(activeTrade);
                        logger.log(Level.INFO, "Long trade hit the stop loss. User ID: {0}", activeTrade.getUserId());
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error while exiting the trade for User Id: {0}", activeTrade.getUserId());
                    }
                }
            } else {
                logger.log(Level.INFO, "No active long trades found.");
            }

            if (activeTrade != null && activeTrade.getPredictedTrend().equals("SHORT")) {
                if (ltp <= activeTrade.getTarget()) {
                    try {
                        List<OrderRequest> orders = activeTrade.getOrderRequests();
                        exitTrade(orders);
                        activeTrade.setStatus("TARGET");
                        tradeDetailsService.saveTradeDetails(activeTrade);
                        logger.log(Level.INFO, "Short trade hit the target. User ID: {0}", activeTrade.getUserId());
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error while exiting the trade for User Id: {0}", activeTrade.getUserId());
                    }
                } else if (ltp >= activeTrade.getStopLoss()) {
                    try {
                        List<OrderRequest> orders = activeTrade.getOrderRequests();
                        exitTrade(orders);
                        activeTrade.setStatus("STOPLOSS");
                        tradeDetailsService.saveTradeDetails(activeTrade);
                        logger.log(Level.INFO, "Short trade hit the stop loss. User ID: {0}", activeTrade.getUserId());
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Error while exiting the trade for User Id: {0}", activeTrade.getUserId());
                    }
                }
            } else {
                logger.log(Level.INFO, "No active short trades found.");
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
            priceDataService.deleteAllPriceData();
        } else {
            logger.log(Level.WARNING, "Insufficient number of orders to exit the trade.");
        }
    }

}
