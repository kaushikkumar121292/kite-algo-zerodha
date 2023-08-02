import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class OptionChainFetcher {

    public static void main(String[] args) {

        String instrumentExchange = "NFO";
        String niftyTradingSymbol = "NIFTY";
        String expiry = "23803";
        double NiftyOpen = 19646.05;
        double gap = 50;

        // Fetch 10 nearest option data
        for (int i = -5; i <= 4; i++) {
            double strikePrice = Math.round(NiftyOpen / gap) * gap + (i * gap);
            String peTradingSymbol = generateTradingSymbol(niftyTradingSymbol, expiry, strikePrice, "PE");
            String ceTradingSymbol = generateTradingSymbol(niftyTradingSymbol, expiry, strikePrice, "CE");

            fetchAndPrintOptionData(instrumentExchange, peTradingSymbol);
            fetchAndPrintOptionData(instrumentExchange, ceTradingSymbol);
        }
    }

    private static String generateTradingSymbol(String underlyingTradingSymbol, String expiry, double strikePrice, String optionType) {
        long roundedStrikePrice = Math.round(strikePrice);
        return underlyingTradingSymbol + expiry + roundedStrikePrice + optionType;
    }

    private static void fetchAndPrintOptionData(String instrumentExchange, String instrumentTradingSymbol) {
        try {
            String url = "https://api.kite.trade/quote/ltp?i=" + instrumentExchange + ":" + instrumentTradingSymbol;
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("X-Kite-Version", "3");
            con.setRequestProperty("Authorization", "enctoken " + "QYiEYQOKIDI5BcSu4NS0/dMAhBia+60Mcqzp6dXlivk2OwL0RYYHzjkg5X6zszfrMu0K1b/+Yhs2zAjTPQelEw2dsWyo7eyL+Dm/kIWfN9p8eHzOYstYIA==");
            int responseCode = con.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String optionChainData = response.toString();
                System.out.println("Contract: " + instrumentTradingSymbol);
                System.out.println(optionChainData); // You can further process the data as per your requirements
                System.out.println("-----------------------------");
            } else {
                System.out.println("Error occurred while fetching option chain. HTTP error code: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
