package com.ali.finansbot.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Twitter (X) API kullanmadan, Playwright browser otomasyonu ile tweet atar.
 *
 * Çalışma mantığı:
 * 1. Daha önce kaydedilmiş auth.json (session/cookie) dosyasını yükler
 * 2. Chromium tarayıcısını başlatır
 * 3. x.com'a gider, tweet yazar, "Post" butonuna basar
 * 4. Session'ı günceller (cookie yenileme)
 *
 * İlk kurulumda SessionGenerator ile auth.json oluşturulmalıdır.
 */
@Service
public class TwitterClient {

    private final String sessionPath;
    private final boolean headless;

    /** Tweet gönderimi arasındaki retry sayısı */
    private static final int MAX_RETRIES = 2;

    public TwitterClient(
            @Value("${twitter.session-path:auth.json}") String sessionPath,
            @Value("${twitter.headless:true}") boolean headless) {
        this.sessionPath = sessionPath;
        this.headless = headless;
    }

    /**
     * Playwright ile X.com'a tweet gönderir.
     *
     * @param text tweet içeriği (max 280 karakter)
     */
    public void postTweet(String text) {
        Path session = Paths.get(sessionPath);

        if (!Files.exists(session)) {
            System.out.println("[Twitter] ❌ auth.json bulunamadı! Önce SessionGenerator çalıştırın.");
            System.out.println("[Twitter] Komut: mvn exec:java -Dexec.mainClass=com.ali.finansbot.util.SessionGenerator");
            return;
        }

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                doPostTweet(text, session);
                return; // Başarılı
            } catch (Exception e) {
                System.out.printf("[Twitter] ❌ Tweet gönderilemedi (deneme %d/%d): %s%n",
                        attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleep(5000); // Retry öncesi bekle
                }
            }
        }
        System.out.println("[Twitter] ❌ Tüm denemeler başarısız oldu.");
    }

    private void doPostTweet(String text, Path session) {
        try (Playwright pw = Playwright.create()) {
            Browser browser = pw.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(headless)
                            .setArgs(List.of(
                                    "--disable-blink-features=AutomationControlled",
                                    "--no-sandbox",
                                    "--disable-dev-shm-usage"
                            ))
            );

            BrowserContext context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setStorageStatePath(session)
                            .setViewportSize(1280, 800)
                            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/131.0.0.0 Safari/537.36")
                            .setLocale("tr-TR")
                            .setTimezoneId("Europe/Istanbul")
            );

            Page page = context.newPage();

            // Navigator.webdriver özelliğini gizle (bot algılama önlemi)
            page.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})");

            try {
                // --- 1) Ana sayfaya git ---
                System.out.println("[Twitter] 🌐 x.com'a gidiliyor...");
                page.navigate("https://x.com/home", new Page.NavigateOptions()
                        .setTimeout(60000));
                
                // NETWORKIDLE KALDIRILDI - Twitter arka planda sürekli veri çektiği için timeout'a düşürüyordu.
                humanDelay(3000, 5000);

                // --- 2) Oturum kontrolü ---
                String currentUrl = page.url();
                if (currentUrl.contains("/login") || currentUrl.contains("/i/flow/login")) {
                    System.out.println("[Twitter] ❌ SESSION EXPIRED! auth.json süresi dolmuş.");
                    System.out.println("[Twitter] ⚠️ SessionGenerator'ı tekrar çalıştırıp yeni auth.json oluşturun.");
                    System.out.println("[Twitter] Komut: mvn exec:java -Dexec.mainClass=com.ali.finansbot.util.SessionGenerator");
                    browser.close();
                    throw new RuntimeException("Session expired — yeniden giriş gerekiyor");
                }

                // Cookie consent banner varsa kapat
                dismissCookieBanner(page);

                // --- 3) Tweet compose alanına yaz ---
                System.out.println("[Twitter] ✏️ Tweet yazılıyor...");

                // Compose alanını bul — x.com'da birden fazla textbox olabilir
                Locator composeBox = page.locator("div[data-testid='tweetTextarea_0']");

                // Eğer compose box doğrudan görünmüyorsa, "Post" butonuna tıkla
                if (!composeBox.isVisible()) {
                    // Ana sayfadaki "What is happening?!" alanına tıkla
                    Locator tweetInput = page.locator("div[data-testid='tweetTextarea_0'], " +
                            "div[role='textbox'][data-testid]");
                    if (tweetInput.isVisible()) {
                        tweetInput.click();
                    } else {
                        // Compose post sayfasına doğrudan git
                        page.navigate("https://x.com/compose/post", new Page.NavigateOptions().setTimeout(60000));
                        humanDelay(2000, 4000);
                    }
                    composeBox = page.locator("div[data-testid='tweetTextarea_0']");
                }

                composeBox.waitFor(new Locator.WaitForOptions().setTimeout(10000));
                composeBox.click();
                humanDelay(500, 1000);

                // Karakter karakter yazma — daha doğal görünür
                typeHumanLike(page, composeBox, text);

                humanDelay(1000, 2000);

                // --- 4) Post butonuna bas ---
                System.out.println("[Twitter] 📤 Tweet gönderiliyor...");
                try {
                    Locator postButton = page.locator("button[data-testid='tweetButtonInline'], button[data-testid='tweetButton']").first();
                    postButton.waitFor(new Locator.WaitForOptions().setTimeout(10000));
                    postButton.click();
                } catch (Exception e) {
                    System.out.println("[Twitter] ⚠️ Post butonu bulunamadı, Ctrl+Enter ile gönderim deneniyor...");
                    // Kutuya odaklan ve klavye kısayoluyla gönder (Kurşun geçirmez yöntem)
                    composeBox.focus();
                    composeBox.press("Control+Enter");
                }

                // Tweet'in gönderildiğini bekle
                humanDelay(3000, 5000);

                // --- 5) Başarı kontrolü ---
                // Post sonrası hata mesajı var mı kontrol et
                Locator errorToast = page.locator("[data-testid='toast']");
                if (errorToast.isVisible()) {
                    String toastText = errorToast.textContent();
                    if (toastText != null && toastText.toLowerCase().contains("error")) {
                        throw new RuntimeException("Tweet hatası: " + toastText);
                    }
                }

                // --- 6) Session güncelle ---
                context.storageState(new BrowserContext.StorageStateOptions()
                        .setPath(session));
                System.out.println("[Twitter] 💾 Session güncellendi.");

                System.out.println("[Twitter] ✅ Tweet başarıyla gönderildi! (Playwright)");

            } finally {
                browser.close();
            }
        }
    }

    /**
     * İnsan benzeri yazma — karakter karakter, rastgele gecikmelerle
     */
    private void typeHumanLike(Page page, Locator target, String text) {
        // Emojilerin (Surrogate Pairs) bozulmaması için charArray yerine codePoints kullanıyoruz
        text.codePoints().forEach(cp -> {
            String character = new String(Character.toChars(cp));
            target.pressSequentially(character,
                    new Locator.PressSequentiallyOptions()
                            .setDelay(ThreadLocalRandom.current().nextInt(20, 80)));
        });
    }

    /**
     * Cookie consent banner'ını kapatır (varsa)
     */
    private void dismissCookieBanner(Page page) {
        try {
            // X.com bazen cookie consent gösteriyor
            Locator cookieBtn = page.locator("div[role='button']:has-text('Refuse non-essential cookies'), " +
                    "div[role='button']:has-text('Gerekli olmayan çerezleri reddet')");
            if (cookieBtn.isVisible()) {
                cookieBtn.click();
                humanDelay(1000, 2000);
                System.out.println("[Twitter] 🍪 Cookie banner kapatıldı.");
            }
        } catch (Exception ignored) {
            // Banner yoksa sorun değil
        }
    }

    /**
     * İnsan benzeri rastgele bekleme
     */
    private void humanDelay(int minMs, int maxMs) {
        sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs));
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
