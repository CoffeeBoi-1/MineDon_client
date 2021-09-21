package net.foxpoint.minedon;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.apache.logging.log4j.LogManager;

public class SseClient {
    public static InputStream GetSseInputStream(String urlPath) {
        try {
            URL url = new URL(urlPath);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestMethod("GET");
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setRequestProperty("Charset", "UTF-8");
            urlConnection.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");

            InputStream inputStream = urlConnection.getInputStream();
            InputStream is = new BufferedInputStream(inputStream);
            return is;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void ReadStream(InputStream IS, SseEventHandler handler) {
        try {
            new Thread(() -> {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(IS));
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        handler.HandleEvent(IS, line);
                    }
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
