package com.javatechie.spring.mongo.api.service;

import com.javatechie.spring.mongo.api.model.*;
import com.javatechie.spring.mongo.api.repository.PriceDataRepository;
import com.javatechie.spring.mongo.api.repository.TradeDetailRepositoryThreePm;
import com.javatechie.spring.mongo.api.repository.UserDetailRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ThreePmSchedulerService {

    public static final String IJ_6185 = "IJ6185";

    public static final String INSTRUMENT_EXCHANGE = "NFO";
    public static final String NIFTY_TRADING_SYMBOL = "NIFTY";

    public static final double OPTION_GAP = 50;

    @Autowired
    private PriceDataRepository priceDataRepository;

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
    private OrderService orderService;

    @Autowired
    private PriceDataService priceDataService;

    @Autowired
    private TradeDetailRepositoryThreePm tradeDetailRepositoryThreePm;

    @Autowired
    private UserDetailRepository userDetailRepository;

    @Autowired
    private CandleService candleService;

    @Scheduled(fixedDelay = 100)
    public void ThreePMSchedulerService() throws Exception {
        LocalTime currentTime = LocalTime.now(ZoneId.of("Asia/Kolkata"));
        if (currentTime.isBefore(LocalTime.of(9, 15)) || currentTime.isAfter(LocalTime.of(15, 31))) {
            throw new IllegalStateException("Trading only allowed during Indian trading hours (9:15 AM to 3:30 PM).");
        }
        List<UserDetail> allUser = getAllUser();
        String masterEncryptedToken = allUser.stream().filter(user -> user.getUserId().equalsIgnoreCase(IJ_6185)).findFirst().get().getEncryptedToken();
        String masterExpiry = allUser.stream().filter(user -> user.getUserId().equalsIgnoreCase(IJ_6185)).findFirst().get().getExpiry();
        double ltp = ltpService.getLtp();
        if (!tradeDetailRepositoryThreePm.findByIsActive(true).isEmpty()) {
            throw new RuntimeException("there is a active trade based on ThreePMSchedulerService");
        }
        // Create Maps to store option trading symbols and their corresponding last prices
        Map<String, Double> peOptionsMap = new HashMap<>();
        Map<String, Double> ceOptionsMap = new HashMap<>();

        // Fetch 10 nearest option data
        IntStream.rangeClosed(-5, 4)
                .forEach(i -> {
                    double strikePrice = Math.round(ltp / OPTION_GAP) * OPTION_GAP + (i * OPTION_GAP);
                    String peTradingSymbol = generateTradingSymbol(NIFTY_TRADING_SYMBOL, masterExpiry, strikePrice, "PE");
                    String ceTradingSymbol = generateTradingSymbol(NIFTY_TRADING_SYMBOL, masterExpiry, strikePrice, "CE");

                    try {
                        String peOptionsData = fetchOptionsData(INSTRUMENT_EXCHANGE, peTradingSymbol, masterEncryptedToken);
                        Map<Integer, Double> peIntegerDoubleMap = parseLastPrice(peOptionsData);
                        // Put the PE option trading symbol and last price into the map
                        peOptionsMap.put(peTradingSymbol, peIntegerDoubleMap.entrySet().stream().findFirst().get().getValue());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        String ceOptionsData = fetchOptionsData(INSTRUMENT_EXCHANGE, ceTradingSymbol, masterEncryptedToken);
                        Map<Integer, Double> ceIntegerDoubleMap = parseLastPrice(ceOptionsData);
                        // Put the CE option trading symbol and last price into the map
                        ceOptionsMap.put(ceTradingSymbol, ceIntegerDoubleMap.entrySet().stream().findFirst().get().getValue());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        Map<String, Double> filteredPeOptionsMap = peOptionsMap.entrySet().stream()
                .filter(entry -> entry.getValue() >= 150 && entry.getValue() <= 160)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        // Select the entry with the lowest value if there are multiple entries
        if (filteredPeOptionsMap.size() > 1) {
            Optional<Map.Entry<String, Double>> minEntry = filteredPeOptionsMap.entrySet().stream()
                    .min(Comparator.comparing(Map.Entry::getValue));
            // Get the entry with the lowest value
            minEntry.ifPresent(entry -> {
                filteredPeOptionsMap.clear();
                filteredPeOptionsMap.put(entry.getKey(), entry.getValue());
            });
        }


        Map<String, Double> filteredCeOptionsMap = ceOptionsMap.entrySet().stream()
                .filter(entry -> entry.getValue() >= 150 && entry.getValue() <= 160)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Select the entry with the lowest value if there are multiple entries
        if (filteredCeOptionsMap.size() > 1) {
            Optional<Map.Entry<String, Double>> minEntry = filteredCeOptionsMap.entrySet().stream()
                    .min(Comparator.comparing(Map.Entry::getValue));

            // Get the entry with the lowest value
            minEntry.ifPresent(entry -> {
                filteredCeOptionsMap.clear();
                filteredCeOptionsMap.put(entry.getKey(), entry.getValue());
            });
        }



        if (!filteredCeOptionsMap.isEmpty() && !filteredPeOptionsMap.isEmpty()) {
            allUser.stream().forEach(userDetail -> {
                if (userDetail.getStrategy().equalsIgnoreCase("THREE_PM")) {
                    ArrayList<OrderRequest> orderRequests = new ArrayList<>();
                    orderRequests.add(OrderRequest
                            .builder()
                            .exchange("NFO")
                            .tradingSymbol(filteredCeOptionsMap.entrySet().stream().findFirst().get().getKey())
                            .transactionType("BUY")
                            .orderType("MARKET")
                            .quantity(userDetail.getQuantity())
                            .price("0")
                            .product(userDetail.getProduct())
                            .validity("DAY")
                            .disclosedQuantity("0")
                            .triggerPrice("0")
                            .squareoff("0")
                            .stoploss("0")
                            .trailingStoploss("0")
                            .userId(userDetail.getUserId())
                            .build());
                    orderRequests.add(OrderRequest
                            .builder()
                            .exchange("NFO")
                            .tradingSymbol(filteredPeOptionsMap.entrySet().stream().findFirst().get().getKey())
                            .transactionType("BUY")
                            .orderType("MARKET")
                            .quantity(userDetail.getQuantity())
                            .price("0")
                            .product(userDetail.getProduct())
                            .validity("DAY")
                            .disclosedQuantity("0")
                            .triggerPrice("0")
                            .squareoff("0")
                            .stoploss("0")
                            .trailingStoploss("0")
                            .userId(userDetail.getUserId())
                            .build());

                    try {
                        stopTradingException(userDetail);

                        String jsonDataForCeLeg = getOrderDetails(orderService.placeOrder(orderRequests.get(0), userDetail), userDetail.getEncryptedToken());

                        String jsonDataForPeLeg =getOrderDetails(orderService.placeOrder(orderRequests.get(1), userDetail), userDetail.getEncryptedToken());

                        if(getExecutedPrice(jsonDataForCeLeg) != 0.0 && getExecutedPrice(jsonDataForPeLeg) != 0.0){

                            filteredCeOptionsMap.put(filteredCeOptionsMap.entrySet().stream().findFirst().get().getKey(),getExecutedPrice(jsonDataForCeLeg));

                            filteredPeOptionsMap.put(filteredPeOptionsMap.entrySet().stream().findFirst().get().getKey(),getExecutedPrice(jsonDataForPeLeg));

                        }


                        String ceOptionsDataForSnapshot = fetchOptionsData(INSTRUMENT_EXCHANGE, filteredCeOptionsMap.entrySet().stream().findFirst().get().getKey(), masterEncryptedToken);
                        Map<Integer, Double> ceIntegerDoubleSnapshotMap = parseLastPrice(ceOptionsDataForSnapshot);
                        String peOptionsDataForSnapshot = fetchOptionsData(INSTRUMENT_EXCHANGE, filteredPeOptionsMap.entrySet().stream().findFirst().get().getKey(), masterEncryptedToken);
                        Map<Integer, Double> peIntegerDoubleSnapshotMap = parseLastPrice(peOptionsDataForSnapshot);


                        Candle ceCandle = getOptionCandle(ceIntegerDoubleSnapshotMap.entrySet().stream().findFirst().get().getKey().toString());
                        Candle peCandle = getOptionCandle(peIntegerDoubleSnapshotMap.entrySet().stream().findFirst().get().getKey().toString());

                        tradeDetailRepositoryThreePm.save(TradeDetailForThreePm
                                .builder()
                                .ceLeg(filteredCeOptionsMap)
                                .ceLegEntry(filteredCeOptionsMap.entrySet().stream().findFirst().get().getValue())
                                .ceLegTarget(filteredCeOptionsMap.entrySet().stream().findFirst().get().getValue() + 20)
                                .ceLegSl(filteredCeOptionsMap.entrySet().stream().findFirst().get().getValue() - 5)
                                .peLeg(filteredPeOptionsMap)
                                .peLegEntry(filteredPeOptionsMap.entrySet().stream().findFirst().get().getValue())
                                .peLegTarget(filteredPeOptionsMap.entrySet().stream().findFirst().get().getValue() + 20)
                                .peLegSl(filteredPeOptionsMap.entrySet().stream().findFirst().get().getValue() - 5)
                                .orderRequest(orderRequests)
                                .userId(userDetail.getUserId())
                                .isActive(true)
                                .dateTime(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).toString())
                                .date(LocalDate.now().toString())
                                .candleCe(ceCandle)
                                .candleCe(peCandle)
                                .volumeDiff(ceCandle.getVolume()-peCandle.getVolume())
                                .build());

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

            });
        }
    }

    private Candle getOptionCandle(String instrumentToken ) throws IOException {
        ZoneId zoneId = ZoneId.of("Asia/Kolkata");
        String interval = getMasterUser().getInterval();
        String timeFrom = LocalTime.now(zoneId).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String DateFrom = LocalDate.now(zoneId).toString();
        LocalDate today = LocalDate.now(zoneId);
        String from = DateFrom + " " + timeFrom;
        String to = today + " " + LocalTime.now(zoneId).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        return candleService.parseCandlesFromJson(historicalDataService.getHistoryDataOfInstrument(instrumentToken, from, to, interval)).get(0);
    }

    private static double getExecutedPrice(String jsonData) {
        // Parse the JSON data
        JSONObject jsonObject = new JSONObject(jsonData);

        // Get the "data" array
        JSONArray dataArray = jsonObject.getJSONArray("data");

        // Get averagePrice for the first order with "COMPLETE" status
        return getAveragePriceForStatus(dataArray, "COMPLETE");
    }

    private void stopTradingException(UserDetail userDetail) {
        List<TradeDetailForThreePm> successTradeListForToday = tradeDetailRepositoryThreePm.findByDateAndIsSuccessAndIsActiveAndUserId(LocalDate.now().toString(), true, false, userDetail.getUserId());
        List<TradeDetailForThreePm> unsuccessTradeListForToday = tradeDetailRepositoryThreePm.findByDateAndIsSuccessAndIsActiveAndUserId(LocalDate.now().toString(), false, false, userDetail.getUserId());
        double investedCapital = Double.parseDouble(userDetail.getQuantity()) * 160 * 2;
        double dayTarget = (investedCapital * 21) / 100;
        double dayLossCapacity = (investedCapital * 10) / 100;
        double currentProfit = successTradeListForToday.stream().count() * Double.parseDouble(userDetail.getQuantity()) * 15;
        double currentLoss = unsuccessTradeListForToday.stream().count() * Double.parseDouble(userDetail.getQuantity()) * 10;
        double netProfitOrLoss = currentProfit - currentLoss;
        if (netProfitOrLoss >= dayTarget) {
            throw new RuntimeException("Profit target reached");
        }
        if (netProfitOrLoss <= dayLossCapacity*(-1)) {
            throw new RuntimeException("Loss capacity reached");
        }
    }

    private static String generateTradingSymbol(String underlyingTradingSymbol, String expiry, double strikePrice, String optionType) {
        long roundedStrikePrice = Math.round(strikePrice);
        return underlyingTradingSymbol + expiry + roundedStrikePrice + optionType;
    }

    private static String fetchOptionsData(String instrumentExchange, String instrumentTradingSymbol, String masterEncryptedToken) throws IOException {
        String url = "https://api.kite.trade/quote/ltp?i=" + instrumentExchange + ":" + instrumentTradingSymbol;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("X-Kite-Version", "3");
        con.setRequestProperty("Authorization", "enctoken " + masterEncryptedToken);
        int responseCode = con.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                return response.toString();
            }
        } else {
            return null;
        }
    }

    private static Map<Integer, Double> parseLastPrice(String optionsData) {
        Map<Integer, Double> resultMap = null;
        try {
            JSONObject jsonObject = new JSONObject(optionsData);

            // Extracting data node
            JSONObject dataNode = jsonObject.getJSONObject("data");

            // Creating a Map to store instrument_token as key and last_price as value
            resultMap = new HashMap<>();

            // Iterating through the keys of the data node
            for (String key : dataNode.keySet()) {
                JSONObject innerNode = dataNode.getJSONObject(key);

                // Extracting instrument_token and last_price values
                int instrumentToken = innerNode.getInt("instrument_token");
                double lastPrice = innerNode.getDouble("last_price");

                // Putting values into the Map
                resultMap.put(instrumentToken, lastPrice);
            }

            // Printing the result Map
            System.out.println("Result Map: " + resultMap);

        } catch (Exception e) {
            e.printStackTrace();
        }


        // Handle the case when the structure is not as expected
        return resultMap;
    }

    private List<UserDetail> getAllUser() {
        List<UserDetail> userDetailList = userDetailRepository.findAll();
        if (!userDetailList.isEmpty()) {
            return userDetailList;
        } else {
            return null;
        }
    }


    private static double getAveragePriceForStatus(JSONArray dataArray, String targetStatus) {
        for (int i = 0; i < dataArray.length(); i++) {
            JSONObject order = dataArray.getJSONObject(i);

            // Check if the order status matches the targetStatus
            if (targetStatus.equals(order.getString("status"))) {
                // Retrieve and return the "average_price"
                return order.getDouble("average_price");
            }
        }

        // Return a default value if no matching status is found
        return 0.0;
    }



    public static String getOrderDetails(String orderId, String encToken) {
        try {
            // Replace the URL with the actual API endpoint
            String apiUrl = "https://api.kite.trade/orders/" + orderId;

            // Create URL object
            URL url = new URL(apiUrl);

            // Open connection
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set request method
            connection.setRequestMethod("GET");

            connection.setRequestProperty("X-Kite-Version", "3");

            // Set request headers
            connection.setRequestProperty("Authorization", "enctoken " + encToken);

            // Get response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuffer response = new StringBuffer();

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Close connection
            connection.disconnect();

            // Return the response as a string
            return response.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null; // Handle error appropriately in your application
        }
    }


    private UserDetail getMasterUser() {
        Query query = Query.query(Criteria.where("userId").is(IJ_6185));
        UserDetail userDetail = mongoTemplate.findOne(query, UserDetail.class);
        return userDetail;
    }


}

