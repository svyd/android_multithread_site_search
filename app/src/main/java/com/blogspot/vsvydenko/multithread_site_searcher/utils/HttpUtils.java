package com.blogspot.vsvydenko.multithread_site_searcher.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by vsvydenko on 11.09.14.
 */
public class HttpUtils {

    // Given a URL, establishes an HttpUrlConnection and retrieves
// the web page content as a InputStream, which it returns as
// a string.
    public static String doRequest(String myUrl) throws IOException {
        InputStream is = null;
        StringBuilder contentAsString = new StringBuilder();

        try {
            URL url = new URL(myUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            if (response != 200) {
                contentAsString.append("Error occured! ");
                return contentAsString.append("Code: " + response).toString();
            } else {
                is = new BufferedInputStream(conn.getInputStream());
            }


            // Convert the InputStream into a string
            contentAsString.append(readIt(is));
            return contentAsString.toString();

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    // Reads an InputStream and converts it to a String.
    public static String readIt(InputStream stream) throws IOException,
            UnsupportedEncodingException {
        StringBuilder response = new StringBuilder();
        InputStream is = null;
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            for (String line; (line = bufferedReader.readLine()) != null;) {
                response.append(line);
            }
            return response.toString();
        } finally {
            if (is != null) {
                is.close();
            }
        }

    }
}
