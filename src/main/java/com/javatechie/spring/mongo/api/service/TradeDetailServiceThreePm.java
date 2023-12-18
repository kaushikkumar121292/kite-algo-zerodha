package com.javatechie.spring.mongo.api.service;

import com.javatechie.spring.mongo.api.model.TradeDetailForThreePm;
import com.javatechie.spring.mongo.api.repository.TradeDetailRepositoryThreePm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TradeDetailServiceThreePm {

    private final TradeDetailRepositoryThreePm tradeDetailRepositoryThreePm;

    @Autowired
    public TradeDetailServiceThreePm(TradeDetailRepositoryThreePm tradeDetailRepositoryThreePm) {
        this.tradeDetailRepositoryThreePm = tradeDetailRepositoryThreePm;
    }

    public void saveTradeDetail(TradeDetailForThreePm tradeDetail) {
        tradeDetailRepositoryThreePm.save(tradeDetail);
    }
}
