package com.javatechie.spring.mongo.api.service;

import com.javatechie.spring.mongo.api.model.TradeDetails;
import com.javatechie.spring.mongo.api.model.UserDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class TrailingStopLossScheduler {

    @Autowired
    private TradeDetailsService tradeDetailsService;

    @Autowired
    private UserDetailService userDetailService;

    @Autowired
    private LtpService ltpService;

    @Scheduled(fixedDelay = 500)
    public void startScheduler() throws IOException {

        List<UserDetail> allUserDetails = userDetailService.getAllUserDetails();
        for (UserDetail userDetail : allUserDetails) {
            List<TradeDetails> activeTradesForUser = tradeDetailsService.getActiveTradesForUser(userDetail.getUserId());
            for (TradeDetails tradeDetails : activeTradesForUser) {

                if (tradeDetails.getPredictedTrend().equalsIgnoreCase("LONG")) {
                    double currentPrice = ltpService.getLtp();
                    // Calculate the stop-loss levels based on the trailing logic
                    double stopLoss = calculateTrailingStopLossForLong(tradeDetails.getRisk(),tradeDetails.getEntry(),tradeDetails.getStopLoss(), currentPrice);
                    // Update the stop-loss in the trade details
                    tradeDetails.setStopLoss(stopLoss);
                    tradeDetailsService.saveTradeDetails(tradeDetails);
                }
                // Similarly, handle the "SHORT" case
                if (tradeDetails.getPredictedTrend().equalsIgnoreCase("SHORT")) {
                    double currentPrice = ltpService.getLtp();
                    // Calculate the stop-loss levels based on the trailing logic
                    double stopLoss = calculateTrailingStopLossForShort(tradeDetails.getRisk(),tradeDetails.getEntry(),tradeDetails.getStopLoss(), currentPrice);
                    // Update the stop-loss in the trade details
                    tradeDetails.setStopLoss(stopLoss);
                    tradeDetailsService.saveTradeDetails(tradeDetails);
                }
            }
        }
    }

    private double calculateTrailingStopLossForLong(double risk,double entry, double stopLoss, double currentPrice) {
        if(currentPrice>=entry+(risk*1)){
            return entry;
        }
        if(currentPrice>=entry+(risk*2)){
            return entry+(risk*1);
        }
        return stopLoss;
    }


    private double calculateTrailingStopLossForShort(double risk, double entry, double stopLoss, double currentPrice) {
        if(currentPrice<=entry-(risk*1)){
            return entry;
        }
        if(currentPrice<=entry-(risk*2)){
            return entry-(risk*1);
        }
        return stopLoss;
    }
}
