package com.ali.finansbot.util;

import com.microsoft.playwright.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

/**
 * X.com (Twitter) oturum dosyası oluşturucu.
 *
 * Bu programı bir kez çalıştırıp X.com'a elle giriş yapmanız yeterlidir.
 * Giriş yaptıktan sonra cookie/session bilgileri auth.json dosyasına kaydedilir.
 * Bu dosya daha sonra TwitterClient tarafından otomatik tweet atarken kullanılır.
 *
 * ÖNEMLİ: Bilgisayarınızda Chrome yüklü olmalıdır.
 * Playwright kendi Chromium'u yerine gerçek Chrome'u kullanır — bot algılamayı önler.
 *
 * Kullanım:
 *   mvn compile exec:java -Dexec.mainClass=com.ali.finansbot.util.SessionGenerator
 */
public class SessionGenerator {

    public static void main(String[] args) {
        String outputFile = args.length > 0 ? args[0] : "auth.json";
        Path outputPath = Paths.get(outputFile);

        System.out.println();
        System.out.println("=============================================================");
        System.out.println("          X.com Session Olusturucu                           ");
        System.out.println("=============================================================");
        System.out.println();
        System.out.println("  1. Gercek Chrome tarayici acilacak (bot algilanmaz)");
        System.out.println("  2. x.com/login sayfasina gidecek");
        System.out.println("  3. Kullanici adi ve sifrenizi girin");
        System.out.println("  4. 2FA varsa onaylayin");
        System.out.println("  5. Ana sayfayi (timeline) gordugunuzde");
        System.out.println("     terminale donup ENTER'a basin");
        System.out.println();
        System.out.println("  Session dosyasi: " + outputFile);
        System.out.println();
        System.out.println("=============================================================");
        System.out.println();

        try (Playwright pw = Playwright.create()) {
            // Edge tarayıcısını kullan (Bot algılamayı aşmak için gerçek tarayıcı)
            Browser browser = pw.chromium().launch(
                new BrowserType.LaunchOptions()
                    .setChannel("msedge")
                    .setHeadless(false)
                    .setArgs(List.of(
                        "--disable-blink-features=AutomationControlled",
                        "--start-maximized"
                    ))
            );

            BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                    .setViewportSize(1366, 768)
                    .setLocale("tr-TR")
                    .setTimezoneId("Europe/Istanbul")
            );

            Page page = context.newPage();

            // Daha gelişmiş bot gizleme scripti
            page.addInitScript("""
                delete navigator.__proto__.webdriver;
                window.chrome = { runtime: {} };
                Object.defineProperty(navigator, 'languages', { get: () => ['tr-TR', 'tr', 'en-US', 'en'] });
                """);

            System.out.println("[*] Edge aciliyor... x.com/login");
            page.navigate("https://x.com/i/flow/login");

            System.out.println();
            System.out.println("[*] TALİMAT:");
            System.out.println("    1. Acilan Edge penceresinde giris yapmayi deneyin.");
            System.out.println("    2. Ana sayfaya ulastiginizda bu terminale donup ENTER'a basin.");
            System.out.println();

            Scanner scanner = new Scanner(System.in);
            System.out.print(">>> Giris tamamlandi mi? ENTER'a basin: ");
            scanner.nextLine();

            // Session'ı kaydet
            context.storageState(new BrowserContext.StorageStateOptions().setPath(outputPath));
            
            browser.close();

            System.out.println();
            System.out.println("=============================================================");
            System.out.println("  [OK] Session basariyla kaydedildi!");
            System.out.println("  Dosya: " + outputPath.toAbsolutePath());
            System.out.println("=============================================================");

        } catch (Exception e) {
            System.out.println("[HATA] " + e.getMessage());
            e.printStackTrace();
        }
    }
}
