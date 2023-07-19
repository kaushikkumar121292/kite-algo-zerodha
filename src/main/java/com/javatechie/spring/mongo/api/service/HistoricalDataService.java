package com.javatechie.spring.mongo.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.javatechie.spring.mongo.api.model.*;
import com.javatechie.spring.mongo.api.repository.PriceDataRepository;
import com.javatechie.spring.mongo.api.repository.UserDetailRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@Service
public class HistoricalDataService {

    public static final String IJ_6185 = "IJ6185";


    private final Gson gson = new Gson();

    private final String rootUrl = "https://api.kite.trade";
    private Map<String, String> headers;

    @Autowired
    private UserDetailRepository userDetailRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TradeDetailsService tradeDetailsService;

    @Autowired
    private LtpService ltpService;

    public String getHistoryDataOfInstrument(String instrumentToken, String from, String to, String interval) throws IOException {
        headers = getHeadersWithEnctoken();
        String url = getHistoricalDataUrl(instrumentToken, from, to, interval);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();

        }

        return null; // Return null or handle the error condition as per your requirement
    }



    private String getHistoricalDataUrl(String instrumentToken, String from, String to, String interval) {
        String url = "https://api.kite.trade/instruments/historical/" + instrumentToken + "/" + interval +
                "?from=" + from +
                "&to=" + to;
        return url;
    }




    public Double getFiveEma(String instrumentToken, String from, String to, String interval) throws IOException {
        double ema = 0;
        headers = getHeadersWithEnctoken();

        String url = getHistoricalDataUrl(instrumentToken, from, to, interval);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String responseBody = response.toString();

            JSONObject json = new JSONObject(responseBody);
            JSONArray candles = json.getJSONObject("data").getJSONArray("candles");

            List<Double> closePrices = new ArrayList<>();
            for (int i = 0; i < candles.length(); i++) {
                JSONArray candle = candles.getJSONArray(i);
                double closePrice = candle.getDouble(4); // Close price is at index 4
                closePrices.add(closePrice);
            }

            ema = calculateEMA(closePrices, 5);
        }

        return ema;
    }



    public static double calculateEMA(List<Double> closingPrices, int period) {
        double multiplier = 2.0 / (period + 1);

        double emaPrevious = closingPrices.get(0); // Closing price of the oldest candle
        double emaLatest = 0.0;

        for (int i = 1; i < closingPrices.size(); i++) {
            emaLatest = (closingPrices.get(i) - emaPrevious) * multiplier + emaPrevious;
            emaPrevious = emaLatest;
        }

        return emaLatest;
    }

    public CandleData getLatestCandle(String instrumentToken, String from, String to, String interval) throws IOException {
        headers = getHeadersWithEnctoken();

        String url = getHistoricalDataUrl(instrumentToken, from, to, interval);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String responseBody = response.toString();

            JSONObject json = new JSONObject(responseBody);
            JSONArray candles = json.getJSONObject("data").getJSONArray("candles");

            // Get the latest candle
            JSONArray latestCandle = candles.getJSONArray(candles.length() - 1);
            String timestamp = latestCandle.getString(0); // Timestamp is at index 0
            double open = latestCandle.getDouble(1); // Open price is at index 1
            double high = latestCandle.getDouble(2); // High price is at index 2
            double low = latestCandle.getDouble(3); // Low price is at index 3
            double close = latestCandle.getDouble(4); // Close price is at index 4
            int volume = latestCandle.getInt(5); // Volume is at index 5

            CandleData candleData = new CandleData(timestamp, open, high, low, close, volume);
            return candleData;
        }

        return null; // Return null or handle the error condition as per your requirement
    }

    public boolean isAlertCandle(String instrumentToken, String from, String to, String interval) throws IOException {
        double ema = getFiveEma(instrumentToken, from, to, interval);
        headers = getHeadersWithEnctoken();

        String url = getHistoricalDataUrl(instrumentToken, from, to, interval);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            String responseBody = response.toString();

            JSONObject json = new JSONObject(responseBody);
            JSONArray candles = json.getJSONObject("data").getJSONArray("candles");

            // Get the latest candle
            JSONArray latestCandle = candles.getJSONArray(candles.length() - 1);
            double high = latestCandle.getDouble(2); // High price is at index 2
            double low = latestCandle.getDouble(3); // Low price is at index 3

            if (high < ema || low > ema) {
                return true; // Alert candle
            }
        } else {
            System.out.println("Failed to retrieve historical data. Error: " + responseCode);
        }
        return false; // Not an alert candle
    }








    private Map<String, String> getHeadersWithEnctoken() {
        Map<String, String> headers = new HashMap<>();
        Query query = Query.query(Criteria.where("userId").is(IJ_6185));
        UserDetail userDetail = mongoTemplate.findOne(query, UserDetail.class);
        headers.put("Authorization", "enctoken " + userDetail.getEncryptedToken());
        return headers;
    }
}
