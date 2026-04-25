# 📊 Finans Bot

Türkiye piyasalarını takip eden ve otomatik tweet atan Spring Boot uygulaması.

> **⚡ API'siz çalışır!** Twitter API ücretli olduğu için Playwright browser otomasyonu kullanır. Tamamen ücretsiz.

## 🎯 Özellikler

- 💰 **Gram Altın** (TL)
- 🪙 **Ons Altın** (USD)
- 📈 **BIST 100** endeksi
- 💵 **USD/TRY** döviz kuru
- 💶 **EUR/TRY** döviz kuru
- 🪙 **BTC/USD** Bitcoin fiyatı

## 🚀 Nasıl Çalışır?

1. **GitHub Actions** her 6 saatte bir uygulamayı başlatır (00:00, 06:00, 12:00, 18:00 UTC)
2. Uygulama başladıktan sonra **her saat başı (:00)** tweet atar
3. 6 saat boyunca çalıştıktan sonra otomatik olarak kapanır
4. Döngü tekrar başlar

### Tweet Nasıl Atılır?

```
[Eski Yöntem] Java → Twitter API v2 (OAuth 1.0a) → Tweet  ❌ ÜCRETLİ
[Yeni Yöntem] Java → Playwright → Chromium Browser → x.com → Tweet  ✅ ÜCRETSİZ
```

Playwright, bir Chromium tarayıcısını programatik olarak kontrol eder.  
Daha önce kaydedilmiş session (cookie) ile x.com'a giriş yapar, tweet yazar ve Post butonuna basar.

## ⚙️ İlk Kurulum (Bir Kerelik)

### 1. Playwright Browser İndir

```bash
mvn compile exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

### 2. X.com Session Oluştur

```bash
mvn compile exec:java -e -Dexec.mainClass=com.ali.finansbot.util.SessionGenerator
```

Bu komut:
1. Chromium tarayıcı açar
2. X.com login sayfasına gider
3. **Elle giriş yaparsınız** (kullanıcı adı + şifre + varsa 2FA)
4. Ana sayfa göründüğünde terminale dönüp **ENTER** tuşuna basarsınız
5. `auth.json` dosyası oluşur (cookie/session bilgileri)

> ⚠️ **auth.json hassas veridir! GitHub'a push etmeyin!** (.gitignore'a ekli)

### 3. GitHub Actions İçin Secret Ayarlama

`auth.json` dosyasını base64'e çevirip GitHub Secret olarak ekleyin:

**PowerShell:**
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("auth.json")) | Set-Clipboard
```

**Linux/Mac:**
```bash
base64 -w 0 auth.json | xclip
```

Sonra: **GitHub Repo → Settings → Secrets and variables → Actions → New repository secret**
- Name: `TWITTER_SESSION_B64`
- Value: Kopyaladığınız base64 string

### 4. Local Çalıştırma

```bash
# Uygulamayı çalıştırın (auth.json aynı dizinde olmalı)
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
- **Playwright for Java** - Browser otomasyonu (API'siz tweet atma)
- **GitHub Actions** - Otomatik çalıştırma

## 📅 Zamanlama

- **Gündüz:** Her saat başı tweet
- **GitHub Actions:** 6 saatte bir yeniden başlatma
- **Timezone:** Europe/Istanbul (UTC+3)

## 🔒 Güvenlik

- Session dosyası (`auth.json`) **asla** repo'da saklanmaz
- GitHub Secrets üzerinden base64 olarak gelir
- `.gitignore`'da auth.json eklendi
- Bot algılama önlemleri: gerçekçi user-agent, insan benzeri yazma, random delay

## 🔄 Session Yenileme

X.com session cookie'leri yaklaşık **30-90 gün** geçerlidir.  
Session süresi dolduğunda:

1. Uygulama loglarında `❌ SESSION EXPIRED` mesajı görürsünüz
2. `SessionGenerator`'ı tekrar çalıştırın (Adım 2)
3. Yeni `auth.json`'ı GitHub Secret'a güncelleyin (Adım 3)

## ⚠️ Uyarılar

- X.com otomasyon algılama yapabilir — düşük hacimli kullanım önerilir
- X.com arayüz değişikliklerinde selector'lar güncellenmesi gerekebilir
- Bu araç kişisel/bilgilendirme amaçlı kullanım içindir

## 📜 Lisans

Bu proje açık kaynaklıdır ve serbestçe kullanılabilir.