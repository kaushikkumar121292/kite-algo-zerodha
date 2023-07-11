package com.javatechie.spring.mongo.api.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExtractHighLowFromJSONObjectService {

    public List<Double> getHighLowList(String jsonResponse) {
        List<Double> highLowList = new ArrayList<>();
        String jsonData = jsonResponse;
        List<Map<String, Object>> candleDataList = getCandleDataList(jsonData);

        String previousCandleType = null;
        double previousHigh = 0.0;
        double previousLow = 0.0;
        boolean foundPair = false;

        for (int i = candleDataList.size() - 2; i >= 0; i--) {
            Map<String, Object> currentCandleData = candleDataList.get(i);

            String currentCandleType = (String) currentCandleData.get("CandleType");

            if (previousCandleType != null && !currentCandleType.equals(previousCandleType)) {
                double currentHigh = (double) currentCandleData.get("High");
                double currentLow = (double) currentCandleData.get("Low");
                highLowList.add(previousHigh);
                highLowList.add(previousLow);
                highLowList.add(currentHigh);
                highLowList.add(currentLow);

                foundPair = true;
                break;
            }

            previousCandleType = currentCandleType;
            previousHigh = (double) currentCandleData.get("High");
            previousLow = (double) currentCandleData.get("Low");
        }

        if (!foundPair) {
            // Handle the case where no pair was found
            // You can add your custom logic here
        }

        return highLowList;
    }




    public static List<Map<String, Object>> getCandleDataList(String jsonData) {
        List<Map<String, Object>> candleDataList = new ArrayList<>();

        // Parse JSON data
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);
        JsonArray candlesArray = jsonObject.getAsJsonObject("data").getAsJsonArray("candles");

        // Store candle data in hashmaps
        for (JsonElement candleElement : candlesArray) {
            JsonArray candle = candleElement.getAsJsonArray();
            String timestamp = candle.get(0).getAsString();
            double open = candle.get(1).getAsDouble();
            double high = candle.get(2).getAsDouble();
            double low = candle.get(3).getAsDouble();
            double close = candle.get(4).getAsDouble();

            String candleType = close > open ? "GREEN" : "RED";

            Map<String, Object> candleData = new HashMap<>();
            candleData.put("CandleType", candleType);
            candleData.put("Timestamp", timestamp);
            candleData.put("Open", open);
            candleData.put("High", high);
            candleData.put("Low", low);
            candleData.put("Close", close);

            candleDataList.add(candleData);
        }

        return candleDataList;
    }
}
