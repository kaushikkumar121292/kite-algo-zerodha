package com.javatechie.spring.mongo.api.service;

import com.javatechie.spring.mongo.api.model.PriceDataInsideCandle;
import com.javatechie.spring.mongo.api.model.PriceDataTraffic;
import com.javatechie.spring.mongo.api.repository.PriceDataInsideCandleRepository;
import com.javatechie.spring.mongo.api.repository.PriceDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class PriceDataInsideCandleService {

    private final PriceDataInsideCandleRepository priceDataRepository;

    @Autowired
    public PriceDataInsideCandleService(PriceDataInsideCandleRepository priceDataRepository) {
        this.priceDataRepository = priceDataRepository;
    }

    public ResponseEntity<PriceDataInsideCandle> getLatestPriceData() {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(0, 1, sort);
        Page<PriceDataInsideCandle> page = priceDataRepository.findAll(pageable);
        if (page.hasContent()) {
            PriceDataInsideCandle latestPriceData = page.getContent().get(0);
            return ResponseEntity.ok(latestPriceData);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public void deleteAllPriceData() {
        priceDataRepository.deleteAll();
    }
}
