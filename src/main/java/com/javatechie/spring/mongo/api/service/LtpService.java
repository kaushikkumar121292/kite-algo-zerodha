package com.javatechie.spring.mongo.api.service;
import com.google.gson.Gson;
import com.javatechie.spring.mongo.api.model.LtpQuotes;
import com.javatechie.spring.mongo.api.model.UserDetail;
import com.javatechie.spring.mongo.api.repository.UserDetailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LtpService {

    private final String rootUrl = "https://api.kite.trade";
    private final Gson gson = new Gson();

    @Autowired
    private UserDetailRepository userDetailRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public Double getLtp() throws IOException {
        String url = rootUrl + "/quote/ltp?i=NSE:NIFTY+50";
        Map<String, String> headers = getHeadersWithEnctoken();

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

            String json = response.toString();
            LtpQuotes ltpQuotes = gson.fromJson(json, LtpQuotes.class);
            return ltpQuotes.getData().getNseNifty50().getLastprice();
        } else {
            // Handle error response
            return null;
        }
    }

    private Map<String, String> getHeadersWithEnctoken() {
        Map<String, String> headers = new HashMap<>();
        List<UserDetail> userDetailList = mongoTemplate.find(
                Query.query(new Criteria()).with(Sort.by(Sort.Direction.DESC, "createdDateTime")).limit(1),
                UserDetail.class
        );
        headers.put("Authorization", "enctoken " + userDetailList.get(0).getEncryptedToken());
        return headers;
    }
}
