package com.javatechie.spring.mongo.api.service;

import com.javatechie.spring.mongo.api.model.OrderRequest;
import com.javatechie.spring.mongo.api.model.TradeDetailForThreePm;
import com.javatechie.spring.mongo.api.model.UserDetail;
import com.javatechie.spring.mongo.api.repository.PriceDataRepository;
import com.javatechie.spring.mongo.api.repository.TradeDetailRepositoryThreePm;
import com.javatechie.spring.mongo.api.repository.UserDetailRepository;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ThreePmTradeExitSchedulerService {

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


    @Scheduled(fixedDelay = 500)
    public void threePmTradeExitTask() {
        List<TradeDetailForThreePm> byIsActive = tradeDetailRepositoryThreePm.findByIsActive(true);
        if (byIsActive.isEmpty()) {
            throw new RuntimeException("there is no threePmTradeExitTask found");
        }
        byIsActive.stream().forEach(tradeDetailForThreePm -> {
            double ceLegLtp = 0;
            double peLegLtp = 0;
            try {
                ceLegLtp = parseLastPrice(fetchOptionsLtp("NFO", tradeDetailForThreePm.getCeLeg().keySet().iterator().next(),userDetailRepository.findById(tradeDetailForThreePm.getUserId()).get().getEncryptedToken()));
                peLegLtp = parseLastPrice(fetchOptionsLtp("NFO", tradeDetailForThreePm.getPeLeg().keySet().iterator().next(),userDetailRepository.findById(tradeDetailForThreePm.getUserId()).get().getEncryptedToken()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (ceLegLtp >= tradeDetailForThreePm.getCeLegTarget() || ceLegLtp <= tradeDetailForThreePm.getCeLegSl() && tradeDetailForThreePm.getCeLeg().values().iterator().next()!=0) {
                //place order for exit
                List<OrderRequest> orderRequests = tradeDetailForThreePm.getOrderRequest().stream()
                        .filter(ce -> ce.getTradingSymbol().endsWith("CE"))
                        .collect(Collectors.toList());
                OrderRequest ceLeg = orderRequests.get(0);
                ceLeg.setTransactionType("SELL");
                try {
                    orderService.placeOrder(ceLeg, userDetailRepository.findById(tradeDetailForThreePm.getUserId()).get());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                HashMap<String, Double> updateCeMap = new HashMap<>();
                updateCeMap.put(tradeDetailForThreePm.getCeLeg().keySet().iterator().next(),0.0);
                tradeDetailForThreePm.setCeLeg(updateCeMap);
                tradeDetailRepositoryThreePm.save(tradeDetailForThreePm);
            }

            if (peLegLtp >= tradeDetailForThreePm.getPeLegTarget() || peLegLtp <= tradeDetailForThreePm.getPeLegSl() && tradeDetailForThreePm.getPeLeg().values().iterator().next()!=0) {
                //place order for exit
                List<OrderRequest> orderRequests = tradeDetailForThreePm.getOrderRequest().stream()
                        .filter(pe -> pe.getTradingSymbol().endsWith("PE"))
                        .collect(Collectors.toList());
                OrderRequest peLeg = orderRequests.get(0);
                peLeg.setTransactionType("SELL");
                try {
                    orderService.placeOrder(peLeg, userDetailRepository.findById(tradeDetailForThreePm.getUserId()).get());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                //update the tradeDetailForThreePm
                HashMap<String, Double> updatePeMap = new HashMap<>();
                updatePeMap.put(tradeDetailForThreePm.getPeLeg().keySet().iterator().next(),0.0);
                tradeDetailForThreePm.setPeLeg(updatePeMap);
                tradeDetailRepositoryThreePm.save(tradeDetailForThreePm);

            }
            if(tradeDetailForThreePm.getCeLeg().values().iterator().next()==0 && tradeDetailForThreePm.getPeLeg().values().iterator().next()==0){
                tradeDetailForThreePm.setActive(false);
                tradeDetailRepositoryThreePm.save(tradeDetailForThreePm);
            }
        });

    }


    private static double parseLastPrice(String optionsData) {
        if (optionsData != null) {
            JSONObject json = new JSONObject(optionsData);

            // Check if there is a "data" object and if it contains the expected information
            if (json.has("data")) {
                JSONObject data = json.getJSONObject("data");

                // Iterate over the keys in the "data" object
                for (String key : data.keySet()) {
                    JSONObject instrumentData = data.getJSONObject(key);

                    // Check if the "instrument_data" object contains the "last_price" field
                    if (instrumentData.has("last_price")) {
                        return instrumentData.getDouble("last_price");
                    }
                }
            }
        }

        // Handle the case when the structure is not as expected
        return Double.NaN;
    }


    private static String fetchOptionsLtp(String instrumentExchange, String instrumentTradingSymbol, String encToken) throws IOException {
        String url = "https://api.kite.trade/quote/ltp?i=" + instrumentExchange + ":" + instrumentTradingSymbol;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("X-Kite-Version", "3");
        con.setRequestProperty("Authorization", "enctoken " + encToken);
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


}

