package com.talfaza.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PersistentCookieJar implements CookieJar {
    private final SharedPreferences sharedPreferences;
    private static final String PREF_COOKIES = "cookies";

    public PersistentCookieJar(Context context) {
        sharedPreferences = context.getSharedPreferences("CookiePrefs", Context.MODE_PRIVATE);
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        Set<String> cookieSet = new HashSet<>();
        for (Cookie cookie : cookies) {
            cookieSet.add(cookie.toString());
        }
        sharedPreferences.edit().putStringSet(PREF_COOKIES, cookieSet).apply();
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookies = new ArrayList<>();
        Set<String> cookieSet = sharedPreferences.getStringSet(PREF_COOKIES, new HashSet<>());
        for (String cookieString : cookieSet) {
            Cookie cookie = Cookie.parse(url, cookieString);
            if (cookie != null) {
                cookies.add(cookie);
            }
        }
        return cookies;
    }
} 