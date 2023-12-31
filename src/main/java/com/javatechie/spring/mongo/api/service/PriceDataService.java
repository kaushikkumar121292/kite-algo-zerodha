package com.javatechie.spring.mongo.api.service;

import com.javatechie.spring.mongo.api.model.PriceDataTraffic;
import com.javatechie.spring.mongo.api.repository.PriceDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class PriceDataService {

    private final PriceDataRepository priceDataRepository;

    @Autowired
    public PriceDataService(PriceDataRepository priceDataRepository) {
        this.priceDataRepository = priceDataRepository;
    }

    public ResponseEntity<PriceDataTraffic> getLatestPriceData() {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(0, 1, sort);
        Page<PriceDataTraffic> page = priceDataRepository.findAll(pageable);
        if (page.hasContent()) {
            PriceDataTraffic latestPriceData = page.getContent().get(0);
            return ResponseEntity.ok(latestPriceData);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    public void deleteAllPriceData() {
        priceDataRepository.deleteAll();
    }
}
