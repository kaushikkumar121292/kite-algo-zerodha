package com.javatechie.spring.mongo.api.service;

import com.javatechie.spring.mongo.api.model.PriceData;
import com.javatechie.spring.mongo.api.repository.PriceDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
public class MarkTrafficLightScheduler {


    public static final String INTERVAL = "30minute";
    @Autowired
    private PriceDataRepository priceDataRepository;

    @Autowired
    private HistoricalDataService historicalDataService;

    @Autowired
    private ExtractHighLowFromJSONObjectService extractHighLowFromJSONObjectService;

    @Autowired
    private TradeDetailsService tradeDetailsService;

    @Autowired
    private LtpService ltpService;

    @Autowired
    private PriceDataService priceDataService;

    @Scheduled(fixedDelay = 1000)
    public void markLevelByTrafficLight() throws Exception {
        ZoneId zoneId = ZoneId.of("Asia/Kolkata");
        String instrumentToken = "256265";
        String interval = INTERVAL;
        String timeFrom = "09:15:00";
        String DateFrom = "2023-07-03";
        LocalDate today = LocalDate.now(zoneId);
        String from = DateFrom + " " + timeFrom;
        String to =today +" "+ LocalTime.now(zoneId).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        LocalTime currentTime = LocalTime.now(zoneId);
        if (currentTime.isBefore(LocalTime.of(9, 15)) || currentTime.isAfter(LocalTime.of(15, 31))) {
            throw new IllegalStateException("Marking level is only allowed during Indian trading hours (9:15 AM to 3:30 PM).");
        }
        if(tradeDetailsService.getLatestActiveTradeDetails()!=null ){
            throw new Exception("there is a ongoing trade!!!!");
        }

        String data=historicalDataService.getHistoryDataOfInstrument(instrumentToken, from, to, interval);
        List<Double> levels = extractHighLowFromJSONObjectService.getHighLowList(data);
        Collections.sort(levels);
        PriceData priceData=new PriceData();
        priceData.setLow(levels.get(0));
        priceData.setHigh(levels.get(levels.size()-1));
        if (ltpService.getLtp() < priceData.getLow() || ltpService.getLtp() > priceData.getHigh()) {
            throw new IllegalArgumentException("Price is out of range, not marking any level");
        }

        else if (!priceDataService.getLatestPriceData().hasBody()) {

            priceDataRepository.save(priceData);

        }


    }


}

