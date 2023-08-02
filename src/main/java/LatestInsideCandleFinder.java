import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.javatechie.spring.mongo.api.model.Candle;

import java.util.ArrayList;
import java.util.List;

public class LatestInsideCandleFinder {
    public static void main(String[] args) {
        String jsonData = "{\"status\": \"success\", \"data\": { \"candles\":[[\"2023-07-28T09:15:00+0530\",19659.75,19690.3,19599.45,19636.2,0],[\"2023-07-28T09:45:00+0530\",19637,19666.1,19607.9,19618.5,0],[\"2023-07-28T10:15:00+0530\",19618.65,19639.65,19596.05,19630.35,0],[\"2023-07-28T10:45:00+0530\",19630.3,19642.25,19607.8,19608.55,0],[\"2023-07-28T11:15:00+0530\",19607.7,19616.3,19581.9,19584.55,0],[\"2023-07-28T11:45:00+0530\",19584.5,19603.65,19573.85,19579.95,0],[\"2023-07-28T12:15:00+0530\",19579.6,19609.05,19565.25,19604.85,0],[\"2023-07-28T12:45:00+0530\",19604.9,19611.2,19582.2,19607.75,0],[\"2023-07-28T13:15:00+0530\",19607.9,19607.9,19564.5,19565.6,0],[\"2023-07-28T13:45:00+0530\",19565.6,19598.95,19563.6,19597.25,0],[\"2023-07-28T14:15:00+0530\",19597.7,19628.25,19581.6,19589.85,0],[\"2023-07-28T14:45:00+0530\",19589.8,19663.55,19588.8,19651.9,0],[\"2023-07-28T15:15:00+0530\",19650.95,19656.25,19634,19638,0]]}}";

        List<Candle> candles = parseCandlesFromJson(jsonData);

        Candle latestInsideCandle = findLatestInsideCandle(candles);

        if (latestInsideCandle != null) {
            System.out.println("Latest Inside Candle:");
            System.out.println("Timestamp: " + latestInsideCandle.getTimestamp() + ", Open: " + latestInsideCandle.getOpen()
                    + ", High: " + latestInsideCandle.getHigh() + ", Low: " + latestInsideCandle.getLow() + ", Close: "
                    + latestInsideCandle.getClose() + ", Volume: " + latestInsideCandle.getVolume());
        } else {
            System.out.println("No inside candles found.");
        }
    }

    public static List<Candle> parseCandlesFromJson(String jsonData) {
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

    public static Candle findLatestInsideCandle(List<Candle> candles) {
        for (int i = candles.size() - 1; i > 0; i--) {
            Candle currentCandle = candles.get(i);
            Candle previousCandle = candles.get(i - 1);

            if (isInsideCandle(currentCandle, previousCandle)) {
                return currentCandle;
            }
        }
        return null;
    }

    public static boolean isInsideCandle(Candle currentCandle, Candle previousCandle) {
        return currentCandle.getHigh() <= previousCandle.getHigh() && currentCandle.getLow() >= previousCandle.getLow()
                && currentCandle.getOpen() >= previousCandle.getLow() && currentCandle.getOpen() <= previousCandle.getHigh()
                && currentCandle.getClose() >= previousCandle.getLow() && currentCandle.getClose() <= previousCandle.getHigh();
    }
}
