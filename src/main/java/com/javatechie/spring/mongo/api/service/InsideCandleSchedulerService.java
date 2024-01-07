package com.javatechie.spring.mongo.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javatechie.spring.mongo.api.model.Candle;
import com.javatechie.spring.mongo.api.model.PriceDataInsideCandle;
import com.javatechie.spring.mongo.api.model.PriceDataTraffic;
import com.javatechie.spring.mongo.api.model.UserDetail;
import com.javatechie.spring.mongo.api.repository.PriceDataInsideCandleRepository;
import com.javatechie.spring.mongo.api.repository.UserDetailRepository;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class InsideCandleSchedulerService {

    public static final String IJ_6185 = "IJ6185";


    public Candle secondLastCandle;

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
    private CandleService latestInsideCandleService;


    //@Scheduled(fixedDelay = 500)
    public void markLevelByInsideCandle() throws Exception {
        LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));
        if (currentTime.isBefore(LocalTime.of(9, 15)) || currentTime.isAfter(LocalTime.of(15, 31))) {
            throw new IllegalStateException("Marking level is only allowed during Indian trading hours (9:15 AM to 3:30 PM).");
        }
        ZoneId zoneId = ZoneId.of("Asia/Kolkata");
        String instrumentToken = "256265";
        String interval = getMasterUser().getInterval();
        String timeFrom = "09:15:00";
        String DateFrom = LocalDate.now(zoneId).toString();
        LocalDate today = LocalDate.now(zoneId);
        String from = DateFrom + " " + timeFrom;
        String to = today + " " + LocalTime.now(zoneId).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (tradeDetailsService.getLatestActiveTradeDetails()!=null) {
            throw new Exception("there is an ongoing trade!!!!");
        }
        String data = historicalDataService.getHistoryDataOfInstrument(instrumentToken, from, to, interval);
        List<Candle> motherBabyCandles = latestInsideCandleService.findLatestInsideCandle(data);
        Candle babyCandle = motherBabyCandles.get(0);
        Candle MotherCandle = motherBabyCandles.get(1);
        Boolean flag=checkRedGreenPair(babyCandle,MotherCandle);
        PriceDataInsideCandle priceData = new PriceDataInsideCandle();
        if(flag){
            priceData.setLow(MotherCandle.getLow());
            priceData.setHigh(MotherCandle.getHigh());
        }
        Double ltp = ltpService.getLtp();
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        Pageable pageable = PageRequest.of(0, 1, sort);
        Page<PriceDataInsideCandle> page = priceDataRepository.findAll(pageable);
        if (page.hasContent()) {
            PriceDataInsideCandle latestPriceDataFromDb = page.getContent().get(0);
            if (latestPriceDataFromDb.getHigh() == priceData.getHigh() && latestPriceDataFromDb.getLow() == priceData.getLow()){
                throw new Exception("same price data already exists in db");
            }
        }

        if((secondLastCandle.getClose()!=0.0) &&
                ((secondLastCandle.getHigh()>priceData.getLow() && secondLastCandle.getClose()<priceData.getLow()) || (secondLastCandle.getLow()<priceData.getHigh()&& secondLastCandle.getClose()>priceData.getHigh())) &&
                (secondLastCandle.getClose()>priceData.getHigh() || secondLastCandle.getClose()<priceData.getLow())){
            priceDataRepository.save(priceData);
        }

    }

    private Boolean checkRedGreenPair(Candle babyCandle, Candle motherCandle) {
        // Check if babyCandle is red and motherCandle is green
        if (babyCandle.getOpen() > babyCandle.getClose() && motherCandle.getOpen() < motherCandle.getClose()) {
            return true; // Red-Green pair
        }

        // Check if babyCandle is green and motherCandle is red
        if (babyCandle.getOpen() < babyCandle.getClose() && motherCandle.getOpen() > motherCandle.getClose()) {
            return true; // Green-Red pair
        }

        return false; // Neither red-green nor green-red pair
    }



    private UserDetail getMasterUser() {
        Query query = Query.query(Criteria.where("userId").is(IJ_6185));
        UserDetail userDetail = mongoTemplate.findOne(query, UserDetail.class);
        return userDetail;
    }

    public static double calculateHalfOfMotherCandle(double highPrice, double lowPrice, double ltp) {
        return (highPrice - lowPrice) / 2;
    }


    @Scheduled(cron = "0 31 15 * * *", zone = "Asia/Kolkata")
    public void executeTask() throws Exception {
        if (tradeDetailsService.getLatestActiveTradeDetails() != null) {
            throw new Exception("there is an ongoing trade!!!!");
        }
        priceDataService.deleteAllPriceData();
    }

    //@Scheduled(fixedDelay = 500)
    public void checkCandleClosed() throws Exception {
        ZoneId zoneId = ZoneId.of("Asia/Kolkata");
        String instrumentToken = "256265";
        String interval = getMasterUser().getInterval();
        String timeFrom = "09:15:00";
        String DateFrom = LocalDate.now(zoneId).toString();
        LocalDate today = LocalDate.now(zoneId);
        String from = DateFrom + " " + timeFrom;
        String to = today + " " + LocalTime.now(zoneId).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String jsonData = historicalDataService.getHistoryDataOfInstrument(instrumentToken, from, to, interval);

        try {
            // Create an ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();

            // Parse the JSON string into a JsonNode
            JsonNode jsonNode = objectMapper.readTree(jsonData);

            // Access the "candles" array
            JsonNode candlesNode = jsonNode.get("data").get("candles");

            // Create a list to store Candle objects
            List<Candle> candleList = new ArrayList<>();

            // Iterate through the candlestick data and map it to Candle objects
            for (JsonNode candle : candlesNode) {
                String timestamp = candle.get(0).asText();
                double openPrice = candle.get(1).asDouble();
                double highPrice = candle.get(2).asDouble();
                double lowPrice = candle.get(3).asDouble();
                double closePrice = candle.get(4).asDouble();
                int volume = candle.get(5).asInt();

                // Create a Candle object and add it to the list
                Candle candleObj = Candle.builder()
                        .timestamp(timestamp)
                        .open(openPrice)
                        .high(highPrice)
                        .low(lowPrice)
                        .close(closePrice)
                        .volume(volume)
                        .build();

                candleList.add(candleObj);
            }

            int secondLastIndex = candleList.size() - 2; // Calculate the index of the second last candle

            if (secondLastIndex >= 0) {
                secondLastCandle = candleList.get(secondLastIndex);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }



    }


}
