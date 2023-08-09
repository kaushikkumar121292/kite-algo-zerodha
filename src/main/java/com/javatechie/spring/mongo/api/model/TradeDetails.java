package com.javatechie.spring.mongo.api.model;

import lombok.*;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "TRADE_DETAILS")
public class TradeDetails {
	@Id
	private String tradeId;
	private double entry;
	private double target;
	private double stopLoss;
	private String status;
	private String dateTime;
	private String predictedTrend;
	private double risk;
	private List<OrderRequest> orderRequests;
	private String userId;
	private int trailingCount;

}
