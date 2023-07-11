package com.javatechie.spring.mongo.api.service;

import com.javatechie.spring.mongo.api.model.TradeDetails;
import com.javatechie.spring.mongo.api.repository.TradeDetailsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TradeDetailsService {

    @Autowired
    private TradeDetailsRepository tradeDetailsRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public void saveTradeDetails(TradeDetails tradeDetails) {
        tradeDetailsRepository.save(tradeDetails);
    }

    public TradeDetails getLatestActiveTradeDetails() {
        Query query = new Query(Criteria.where("status").is("ACTIVE"))
                .with(Sort.by(Sort.Direction.DESC, "entry"))
                .limit(1);
        List<TradeDetails> tradeDetailsList = mongoTemplate.find(query, TradeDetails.class);
        if (!tradeDetailsList.isEmpty()) {
            return tradeDetailsList.get(0);
        }
        return null;
    }
}
