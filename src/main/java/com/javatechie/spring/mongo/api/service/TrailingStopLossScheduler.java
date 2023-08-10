package com.javatechie.spring.mongo.api.service;

import com.javatechie.spring.mongo.api.model.TradeDetails;
import com.javatechie.spring.mongo.api.model.Trailing;
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
                    Trailing trailing = calculateTrailingStopLossForLong(tradeDetails.getRisk(), tradeDetails.getEntry(), tradeDetails.getStopLoss()
                            , currentPrice, tradeDetails.getTarget(),tradeDetails.getTrailingCount());
                    // Update the stop-loss in the trade details
                    tradeDetails.setStopLoss(trailing.getTrailingStopLoss());
                    tradeDetails.setTarget(trailing.getTrailingTarget());
                    tradeDetails.setTrailingCount(trailing.getTrailingCount());
                    tradeDetailsService.saveTradeDetails(tradeDetails);
                }
                // Similarly, handle the "SHORT" case
                if (tradeDetails.getPredictedTrend().equalsIgnoreCase("SHORT")) {
                    double currentPrice = ltpService.getLtp();
                    // Calculate the stop-loss levels based on the trailing logic
                    Trailing trailing =calculateTrailingStopLossForShort(tradeDetails.getRisk(),tradeDetails.getEntry(),tradeDetails.getStopLoss()
                            , currentPrice, tradeDetails.getTarget(), tradeDetails.getTrailingCount());
                    // Update the stop-loss in the trade details
                    tradeDetails.setStopLoss(trailing.getTrailingStopLoss());
                    tradeDetails.setTarget(trailing.getTrailingTarget());
                    tradeDetails.setTrailingCount(trailing.getTrailingCount());
                    tradeDetailsService.saveTradeDetails(tradeDetails);
                }
            }
        }
    }

    private Trailing calculateTrailingStopLossForLong(double risk, double entry, double stopLoss, double currentPrice, double target,int trailingCount){
        if(currentPrice>=entry+(risk) && trailingCount==0){
            Trailing trailing = new Trailing();
            trailing.setTrailingStopLoss(stopLoss+(risk));
            trailing.setTrailingTarget(target+(risk));
            trailing.setTrailingCount(trailingCount+1);
            return trailing;
        }
        if(currentPrice>=entry+(risk*2) && trailingCount==1){
            Trailing trailing = new Trailing();
            trailing.setTrailingStopLoss(stopLoss+(risk));
            trailing.setTrailingTarget(target+(risk));
            trailing.setTrailingCount(trailingCount+1);
            return trailing;
        }

        if(currentPrice>=entry+(risk*3) && trailingCount==2){
            Trailing trailing = new Trailing();
            trailing.setTrailingStopLoss(stopLoss+(risk));
            trailing.setTrailingTarget(target+(risk));
            trailing.setTrailingCount(trailingCount+1);
            return trailing;
        }

        return null;
    }


    private Trailing calculateTrailingStopLossForShort(double risk, double entry, double stopLoss, double currentPrice, double target,int trailingCount) {
        if(currentPrice>=entry-(risk) && trailingCount==0){
            Trailing trailing = new Trailing();
            trailing.setTrailingStopLoss(stopLoss-(risk));
            trailing.setTrailingTarget(target-(risk));
            trailing.setTrailingCount(trailingCount+1);
            return trailing;
        }
        if(currentPrice>=entry-(risk*2) && trailingCount==1){
            Trailing trailing = new Trailing();
            trailing.setTrailingStopLoss(stopLoss-(risk));
            trailing.setTrailingTarget(target-(risk));
            trailing.setTrailingCount(trailingCount+1);
            return trailing;
        }
        if(currentPrice>=entry-(risk*3) && trailingCount==2){
            Trailing trailing = new Trailing();
            trailing.setTrailingStopLoss(stopLoss-(risk));
            trailing.setTrailingTarget(target-(risk));
            trailing.setTrailingCount(trailingCount+1);
            return trailing;
        }
        return null;

    }
}
