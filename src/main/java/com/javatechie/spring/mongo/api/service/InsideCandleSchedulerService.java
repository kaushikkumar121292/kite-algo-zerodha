package com.javatechie.spring.mongo.api.service;

import com.javatechie.spring.mongo.api.model.Candle;
import com.javatechie.spring.mongo.api.model.PriceDataInsideCandle;
import com.javatechie.spring.mongo.api.model.PriceDataTraffic;
import com.javatechie.spring.mongo.api.model.UserDetail;
import com.javatechie.spring.mongo.api.repository.PriceDataInsideCandleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class InsideCandleSchedulerService {

    public static final String IJ_6185 = "IJ6185";

    @Autowired
    private PriceDataInsideCandleRepository priceDataRepository;

    @Autowired
    private HistoricalDataService historicalDataService;

    @Autowired
    private ExtractHighLowFromJSONObjectService extractHighLowFromJSONObjectService;

    @Autowired
    private TradeDetailsService tradeDetailsService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private LtpService ltpService;

    @Autowired
    private PriceDataInsideCandleService priceDataService;

    @Autowired
    private LatestInsideCandleService latestInsideCandleService;


    @Scheduled(fixedDelay = 500)
    public void markLevelByInsideCandle() throws Exception {

        LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));
        if (currentTime.isBefore(LocalTime.of(9, 15)) || currentTime.isAfter(LocalTime.of(15, 31))) {
            throw new IllegalStateException("Marking level is only allowed during Indian trading hours (9:15 AM to 3:30 PM).");
        }
        ZoneId zoneId = ZoneId.of("Asia/Kolkata");
        String instrumentToken = "256265";
        String interval = getMasterUser().getInterval();
        String timeFrom = "09:15:00";
        String DateFrom = "2023-07-03";
        LocalDate today = LocalDate.now(zoneId);
        String from = DateFrom + " " + timeFrom;
        String to = today + " " + LocalTime.now(zoneId).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (tradeDetailsService.getLatestActiveTradeDetails()!=null) {
            throw new Exception("there is an ongoing trade!!!!");
        }
        String data = historicalDataService.getHistoryDataOfInstrument(instrumentToken, from, to, interval);
        Candle level = latestInsideCandleService.findLatestInsideCandle(data);
        PriceDataInsideCandle priceData = new PriceDataInsideCandle();
        priceData.setLow(level.getLow());
        priceData.setHigh(level.getHigh());
        Double ltp = ltpService.getLtp();
        if (ltp < priceData.getLow() || ltp > priceData.getHigh()) {
            throw new IllegalArgumentException("Price is out of range, not marking any level for Inside Candle");
        }
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(0, 1, sort);
        Page<PriceDataInsideCandle> page = priceDataRepository.findAll(pageable);
        if (page.hasContent()) {
            PriceDataInsideCandle latestPriceDataFromDb = page.getContent().get(0);
            if (latestPriceDataFromDb.getHigh() == priceData.getHigh() && latestPriceDataFromDb.getLow() == priceData.getLow()){
                throw new Exception("same price data already exists in db");
            }
        }
        priceDataRepository.save(priceData);
    }


    private UserDetail getMasterUser() {
        Query query = Query.query(Criteria.where("userId").is(IJ_6185));
        UserDetail userDetail = mongoTemplate.findOne(query, UserDetail.class);
        return userDetail;
    }

    @Scheduled(cron = "0 31 15 * * *", zone = "Asia/Kolkata")
    public void executeTask() throws Exception {
        if (tradeDetailsService.getLatestActiveTradeDetails() != null) {
            throw new Exception("there is an ongoing trade!!!!");
        }
        priceDataService.deleteAllPriceData();
    }


}
