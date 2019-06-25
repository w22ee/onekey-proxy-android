package me.lixi.store;

import com.google.gson.Gson;

public class GsonFactory {

    private static GsonFactory gsonFactory;
    private static Gson gson;

    public static GsonFactory getInstance() {
        if (gsonFactory == null) {
            gsonFactory = new GsonFactory();
        }
        return gsonFactory;
    }

    private GsonFactory() {
        if (gson == null) {
            gson = new Gson();
        }
    }

    public Gson getGson() {
        return gson;
    }

}