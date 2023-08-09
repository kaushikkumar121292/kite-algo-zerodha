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

                    /*if(trailing.getTrailingStopLoss()== tradeDetails.getStopLoss() && trailing.getTrailingTarget()==tradeDetails.getTarget() && trailing.getTrailingCount()==tradeDetails.getTrailingCount()){
                        throw new RuntimeException("Same trailing data found for long");
                    }*/
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
                    /*if(trailing.getTrailingStopLoss()== tradeDetails.getStopLoss() && trailing.getTrailingTarget()==tradeDetails.getTarget() && trailing.getTrailingCount()==tradeDetails.getTrailingCount()){
                       throw new RuntimeException("Same trailing data found for short");
                    }*/
                    tradeDetailsService.saveTradeDetails(tradeDetails);
                }
            }
        }
    }

    private Trailing calculateTrailingStopLossForLong(double risk, double entry, double stopLoss, double currentPrice, double target,int trailingCount){
        Trailing defTrailing = new Trailing();
        defTrailing.setTrailingStopLoss(stopLoss);
        defTrailing.setTrailingTarget(target);
        defTrailing.setTrailingCount(trailingCount);
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

        return defTrailing;
    }


    private Trailing calculateTrailingStopLossForShort(double risk, double entry, double stopLoss, double currentPrice, double target,int trailingCount) {
        Trailing defTrailing = new Trailing();
        defTrailing.setTrailingStopLoss(stopLoss);
        defTrailing.setTrailingTarget(target);
        defTrailing.setTrailingCount(trailingCount);
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
        return defTrailing;

    }
}
