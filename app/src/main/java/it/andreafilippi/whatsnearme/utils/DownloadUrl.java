package it.andreafilippi.whatsnearme.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadUrl {

    public static String performRequest(String urlParam) throws IOException {
        String urlData = "";
        HttpURLConnection httpURLConnection = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(urlParam);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();

            inputStream = httpURLConnection.getInputStream();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder sb = new StringBuilder();

            String line = "";

            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }

            urlData = sb.toString();

            bufferedReader.close();
        } catch (Exception e) {
            Log.e("URL DOWNLOADER", e.toString());
        } finally {
            if (inputStream != null)
                inputStream.close();

            if (httpURLConnection != null)
                httpURLConnection.disconnect();
        }

        return urlData;
    }

}
