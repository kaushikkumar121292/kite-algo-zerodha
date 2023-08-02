package com.javatechie.spring.mongo.api.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.javatechie.spring.mongo.api.model.Candle;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LatestInsideCandleService {

    public Candle findLatestInsideCandle(String jsonData) {
        List<Candle> candles = parseCandlesFromJson(jsonData);
        return findLatestInsideCandle(candles);
    }

    public List<Candle> parseCandlesFromJson(String jsonData) {
        List<Candle> candles = new ArrayList<>();

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);
        JsonArray candlesArray = jsonObject.getAsJsonObject("data").getAsJsonArray("candles");

        for (JsonElement element : candlesArray) {
            JsonArray candleData = element.getAsJsonArray();
            String timestamp = candleData.get(0).getAsString();
            double open = candleData.get(1).getAsDouble();
            double high = candleData.get(2).getAsDouble();
            double low = candleData.get(3).getAsDouble();
            double close = candleData.get(4).getAsDouble();
            int volume = candleData.get(5).getAsInt();
            candles.add(new Candle(timestamp, open, high, low, close, volume));
        }

        return candles;
    }

    public Candle findLatestInsideCandle(List<Candle> candles) {
        for (int i = candles.size() - 2; i > 0; i--) {
            Candle currentCandle = candles.get(i);
            Candle previousCandle = candles.get(i - 1);

            if (isInsideCandle(currentCandle, previousCandle)) {
                return currentCandle;
            }
        }
        return null;
    }

    public boolean isInsideCandle(Candle currentCandle, Candle previousCandle) {
        return currentCandle.getHigh() <= previousCandle.getHigh() && currentCandle.getLow() >= previousCandle.getLow()
                && currentCandle.getOpen() >= previousCandle.getLow() && currentCandle.getOpen() <= previousCandle.getHigh()
                && currentCandle.getClose() >= previousCandle.getLow() && currentCandle.getClose() <= previousCandle.getHigh();
    }
}
