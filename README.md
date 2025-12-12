# 📊 Finans Bot

Türkiye piyasalarını takip eden ve otomatik tweet atan Spring Boot uygulaması.

## 🎯 Özellikler

- 💰 **Gram Altın** (TL)
- 🪙 **Ons Altın** (USD)
- 📈 **BIST 100** endeksi
- 💵 **USD/TRY** döviz kuru
- 💶 **EUR/TRY** döviz kuru
- 🪙 **BTC/USD** Bitcoin fiyatı

## 🚀 Nasıl Çalışır?

1. **GitHub Actions** her 6 saatte bir uygulamayı başlatır (00:00, 06:00, 12:00, 18:00 UTC)
2. Uygulama başladıktan sonra **her saat başı (:00) ve ortasında (:30)** tweet atar
3. 6 saat boyunca çalıştıktan sonra otomatik olarak kapanır
4. Döngü tekrar başlar

## ⚙️ Kurulum

### GitHub Secrets Ayarları

Repository Settings → Secrets and variables → Actions → New repository secret

Aşağıdaki secret'ları ekleyin:

- `TWITTER_API_KEY`: Twitter API Key
- `TWITTER_API_SECRET`: Twitter API Secret  
- `TWITTER_ACCESS_TOKEN`: Twitter Access Token
- `TWITTER_ACCESS_TOKEN_SECRET`: Twitter Access Token Secret

### Local Çalıştırma

```bash
# Twitter key'lerini environment variable olarak set edin
export TWITTER_API_KEY="your_key"
export TWITTER_API_SECRET="your_secret"
export TWITTER_ACCESS_TOKEN="your_token"
export TWITTER_ACCESS_TOKEN_SECRET="your_token_secret"

# Uygulamayı çalıştırın
mvn spring-boot:run
```

## 📝 Tweet Formatı

```
📊 Piyasa Özeti | 12.12.2025
🕐 23:30

💰 Gram Altın
5.944,02 ₺  📈 +1,19%

🪙 Ons Altın  
4.331,50 $  📈 +1,07%

📈 BIST 100
11.311  📈 +0,69%

💵 USD/TRY
42,68  📈 +0,11%

💶 EUR/TRY
50,17  📈 +0,27%

🪙 BTC/USD
90.095 $  📉 -2,62%

dolar euro altın ons bist100 borsa bitcoin
```

## 🛠️ Teknolojiler

- **Spring Boot 3.5.7** - Backend framework
- **Java 21** - Programming language
- **Yahoo Finance API** - Finansal veri kaynağı
- **Twitter API v2** - Tweet gönderme
- **GitHub Actions** - Otomatik çalıştırma

## 📅 Zamanlama

- **Gündüz:** Her 30 dakikada bir tweet (saat başı ve ortası)
- **GitHub Actions:** 6 saatte bir yeniden başlatma
- **Timezone:** Europe/Istanbul (UTC+3)

## 🔒 Güvenlik

- Twitter API key'leri **asla** kodda saklanmaz
- Tüm hassas veriler GitHub Secrets üzerinden gelir
- `.env` dosyaları `.gitignore`'da

## 📜 Lisans

Bu proje açık kaynaklıdır ve serbestçe kullanılabilir.