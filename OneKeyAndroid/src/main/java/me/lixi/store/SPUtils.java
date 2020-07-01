package me.lixi.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class SPUtils {

    public static void saveAccountInfo(String account, Context context) {
        SharedPreferences preferences = context.getSharedPreferences("account", Context.MODE_PRIVATE);
        String data = preferences.getString("data", "");
        List<String> accountList = new ArrayList<>();
        if (!TextUtils.isEmpty(account)) {
            if (!TextUtils.isEmpty(data)) {
                Gson gson = GsonFactory.getInstance().getGson();
                accountList = gson.fromJson(data, new TypeToken<List<String>>() {
                }.getType());
            }
            if (accountList != null) {
                boolean h = false;
                for (String ips : accountList) {
                    if (ips.equals(account)) {
                        h = true;
                        break;
                    }
                }
                if (!h) {
                    accountList.add(account);
                }
            }
        }
        if (!TextUtils.isEmpty(account)) {
            String content = GsonFactory.getInstance().getGson().toJson(accountList);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("data", content);
            editor.apply();
        }
    }

    public static List<String> getAccountInfo(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("account", Context.MODE_PRIVATE);
        String data = preferences.getString("data", "");
        List<String> accountList = new ArrayList<>();
        if (!TextUtils.isEmpty(data)) {
            Gson gson = GsonFactory.getInstance().getGson();
            accountList = gson.fromJson(data, new TypeToken<List<String>>() {
            }.getType());
        }
        return accountList;
    }
}