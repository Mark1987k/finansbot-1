package com.ali.finansbot.scheduler;

import com.ali.finansbot.service.FinansService;
import com.ali.finansbot.service.TwitterClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class TweetScheduler {

    private final FinansService finansService;
    private final TwitterClient twitterClient;

    private final DecimalFormat df2 = new DecimalFormat("#,##0.00");
    private final DecimalFormat df0 = new DecimalFormat("#,##0");

    public TweetScheduler(FinansService finansService,
            TwitterClient twitterClient,
            @Value("${app.scheduler.cron:0 0 * * * *}") String cron) {
        this.finansService = finansService;
        this.twitterClient = twitterClient;
    }

    @Scheduled(cron = "${app.scheduler.cron:0 0 * * * *}", zone = "Europe/Istanbul")
    public void run() {
        try {
            Map<String, Object> d = finansService.getFinansData();

            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Istanbul"));
            String time = now.format(DateTimeFormatter.ofPattern("HH:mm"));
            String date = now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            String tweet = """
                    📊 Piyasa Özeti | %s
                    🕐 %s

                    💰 Gram Altın
                    %s ₺  %s

                    🪙 Ons Altın
                    %s $  %s

                    📈 BIST 100
                    %s  %s

                    💵 USD/TRY
                    %s  %s

                    💶 EUR/TRY
                    %s  %s

                    🪙 BTC/USD
                    %s $  %s

                    dolar euro altın ons bist100 borsa bitcoin
                    """.formatted(
                    date, time,
                    df2.format(d.get("gramAltin")), formatPct((Double) d.get("gramChangePct")),
                    df2.format(d.get("onsUsd")), formatPct((Double) d.get("onsChangePct")),
                    df0.format(d.get("bist100")), formatPct((Double) d.get("bistChangePct")),
                    df2.format(d.get("usdTry")), formatPct((Double) d.get("usdChangePct")),
                    df2.format(d.get("eurTry")), formatPct((Double) d.get("eurChangePct")),
                    df0.format(d.get("btcUsd")), formatPct((Double) d.get("btcUsdChangePct"))).trim();

            System.out.println(tweet);
            twitterClient.postTweet(tweet);

        } catch (Exception e) {
            System.out.println("[Scheduler] Hata oluştu: " + e.getMessage());
        }
    }

    private String formatPct(double pct) {
        if (pct >= 0) {
            return "📈 +" + df2.format(pct) + "%";
        } else {
            return "📉 -" + df2.format(Math.abs(pct)) + "%";
        }
    }
}
