package com.ali.finansbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinansbotApplication {

   public static void main(String[] args) {
      System.out.println("🤖 Finans Bot başlatılıyor...");
      System.out.println("📅 Tweet zamanları: Her saat başı (:00) ve ortasında (:30)");
      System.out.println("⏳ Uygulama sürekli çalışacak, GitHub Actions 6 saat sonra kapatacak.");
      SpringApplication.run(FinansbotApplication.class, args);
   }
}
