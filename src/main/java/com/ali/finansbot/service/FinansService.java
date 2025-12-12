package com.ali.finansbot.service;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FinansService {

    private final WebClient webClient;

    public FinansService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Map<String, Object> getFinansData() {
        Map<String, Object> data = new HashMap<>();

        Map<String, Object> usdTryData = getYahooFinanceData("USDTRY=X");
        double usdTryToday = extractRegularMarketPrice(usdTryData);
        double usdTryPrev = extractPreviousClose(usdTryData);
        double usdChangePct = calculateChangePct(usdTryToday, usdTryPrev);

        Map<String, Object> eurTryData = getYahooFinanceData("EURTRY=X");
        double eurTryToday = extractRegularMarketPrice(eurTryData);
        double eurTryPrev = extractPreviousClose(eurTryData);
        double eurChangePct = calculateChangePct(eurTryToday, eurTryPrev);

        Map<String, Object> btcUsdData = getYahooFinanceData("BTC-USD");
        double btcUsdToday = extractRegularMarketPrice(btcUsdData);
        double btcUsdPrev = extractPreviousClose(btcUsdData);
        double btcUsdChangePct = calculateChangePct(btcUsdToday, btcUsdPrev);

        Map<String, Object> ons = getYahooFinanceData("GC=F");
        double onsUsd = extractRegularMarketPrice(ons);
        double onsPrev = extractPreviousClose(ons);
        double onsChangePct = calculateChangePct(onsUsd, onsPrev);

        Map<String, Object> bist = getYahooFinanceData("XU100.IS");
        double bist100 = extractRegularMarketPrice(bist);
        double bistPrev = extractPreviousClose(bist);
        double bistChangePct = calculateChangePct(bist100, bistPrev);

        double gramAltin = (onsUsd * usdTryToday) / 31.1035;
        double gramPrev = (onsPrev * usdTryPrev) / 31.1035;
        double gramChangePct = calculateChangePct(gramAltin, gramPrev);

        data.put("usdTry", usdTryToday);
        data.put("eurTry", eurTryToday);
        data.put("btcUsd", btcUsdToday);
        data.put("onsUsd", onsUsd);
        data.put("gramAltin", gramAltin);
        data.put("bist100", bist100);

        data.put("usdChangePct", usdChangePct);
        data.put("eurChangePct", eurChangePct);
        data.put("btcUsdChangePct", btcUsdChangePct);
        data.put("onsChangePct", onsChangePct);
        data.put("bistChangePct", bistChangePct);
        data.put("gramChangePct", gramChangePct);

        return data;
    }

    private Map<String, Object> getYahooFinanceData(String symbol) {
        String uri = String.format("https://query1.finance.yahoo.com/v8/finance/chart/%s?range=1d&interval=1m", symbol);
        @SuppressWarnings("unchecked")
        Map<String, Object> result = webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return result;
    }

    @SuppressWarnings("unchecked")
    private double extractRegularMarketPrice(Map<String, Object> yahooChartJson) {
        if (yahooChartJson == null || !yahooChartJson.containsKey("chart"))
            return 0.0;
        Map<String, Object> chart = (Map<String, Object>) yahooChartJson.get("chart");
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) chart.get("result");
        if (resultList == null || resultList.isEmpty())
            return 0.0;
        Map<String, Object> first = resultList.get(0);
        Map<String, Object> meta = (Map<String, Object>) first.get("meta");
        if (meta == null)
            return 0.0;
        return NumberUtils.toDouble(String.valueOf(meta.get("regularMarketPrice")));
    }

    @SuppressWarnings("unchecked")
    private double extractPreviousClose(Map<String, Object> yahooChartJson) {
        if (yahooChartJson == null || !yahooChartJson.containsKey("chart"))
            return 0.0;
        Map<String, Object> chart = (Map<String, Object>) yahooChartJson.get("chart");
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) chart.get("result");
        if (resultList == null || resultList.isEmpty())
            return 0.0;
        Map<String, Object> first = resultList.get(0);
        Map<String, Object> meta = (Map<String, Object>) first.get("meta");
        if (meta == null)
            return 0.0;
        return NumberUtils.toDouble(String.valueOf(meta.get("previousClose")));
    }

    private double calculateChangePct(double current, double previous) {
        if (previous == 0)
            return 0.0;
        return ((current - previous) / previous) * 100.0;
    }
}
