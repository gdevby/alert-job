package by.gdev.common.service.playwright.sessions.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.SameSiteAttribute;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SessionStorageJsonHelper {

    private static final Gson GSON = new Gson();

    private SessionStorageJsonHelper() {
    }

    public static JsonObject parseRoot(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return GSON.fromJson(json, JsonObject.class);
    }

    public static void restoreCookiesFromRoot(Logger log, BrowserContext context, JsonObject root,
                                       String userUuid, String siteName, String login) {
        if (root == null) {
            return;
        }
        JsonArray cookiesArr = root.getAsJsonArray("cookies");
        if (cookiesArr == null || cookiesArr.isEmpty()) {
            log.info("SESSION: в storageState нет cookies {}/{}/{}", userUuid, siteName, login);
            return;
        }
        List<Cookie> cookies = parseAndDedupeCookies(cookiesArr);
        context.addCookies(cookies);
        log.info("SESSION: {} cookies восстановлены (addCookies) для {}/{}/{}",
                cookies.size(), userUuid, siteName, login);
    }

    public static void restoreOriginsFromRoot(Logger log, Page page, JsonObject root) {
        if (root == null) {
            return;
        }
        JsonArray origins = root.getAsJsonArray("origins");
        if (origins == null || origins.isEmpty()) {
            return;
        }
        for (JsonElement el : origins) {
            JsonObject originObj = el.getAsJsonObject();
            JsonArray lsArr = originObj.getAsJsonArray("localStorage");
            if (lsArr == null || lsArr.isEmpty()) {
                continue;
            }
            String originUrl = originObj.get("origin").getAsString();
            if (originUrl.contains("cloudflare.com")) {
                log.debug("SESSION: пропуск origin {}", originUrl);
                continue;
            }
            Map<String, String> storage = new HashMap<>();
            for (JsonElement kv : lsArr) {
                JsonObject o = kv.getAsJsonObject();
                storage.put(o.get("name").getAsString(), o.get("value").getAsString());
            }
            try {
                page.navigate(originUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                page.evaluate(
                        "s => Object.entries(s).forEach(([k,v]) => localStorage.setItem(k,v))",
                        storage);
                log.info("SESSION: {} ключей localStorage восстановлены для {}", storage.size(), originUrl);
            } catch (Exception e) {
                log.warn("SESSION: localStorage не применён для {}: {}", originUrl, e.getMessage());
            }
        }
    }

    private static List<Cookie> parseAndDedupeCookies(JsonArray cookiesArr) {
        Map<String, Cookie> deduped = new LinkedHashMap<>();
        for (JsonElement el : cookiesArr) {
            JsonObject o = el.getAsJsonObject();
            Cookie cookie = new Cookie(o.get("name").getAsString(), o.get("value").getAsString());
            if (o.has("domain") && !o.get("domain").isJsonNull()) {
                cookie.domain = o.get("domain").getAsString();
            }
            if (o.has("path") && !o.get("path").isJsonNull()) {
                cookie.path = o.get("path").getAsString();
            }
            if (o.has("expires") && !o.get("expires").isJsonNull()) {
                cookie.expires = o.get("expires").getAsDouble();
            }
            if (o.has("httpOnly") && !o.get("httpOnly").isJsonNull()) {
                cookie.httpOnly = o.get("httpOnly").getAsBoolean();
            }
            if (o.has("secure") && !o.get("secure").isJsonNull()) {
                cookie.secure = o.get("secure").getAsBoolean();
            }
            if (o.has("sameSite") && !o.get("sameSite").isJsonNull()) {
                cookie.sameSite = parseSameSite(o.get("sameSite").getAsString());
            }
            String key = cookie.name + "|" + cookie.domain + "|" + cookie.path;
            deduped.put(key, cookie);
        }
        return new ArrayList<>(deduped.values());
    }

    private static SameSiteAttribute parseSameSite(String raw) {
        if (raw == null) {
            return null;
        }
        return switch (raw.toLowerCase()) {
            case "none" -> SameSiteAttribute.NONE;
            case "lax" -> SameSiteAttribute.LAX;
            case "strict" -> SameSiteAttribute.STRICT;
            default -> null;
        };
    }
}
