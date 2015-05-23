package com.transporter.porter;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class ConnectionUtil {

    public static String urlEncodeUTF8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(e);
        }
    }

    public static String urlEncodeUTF8(Map<String,String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String,String> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(String.format("%s=%s", urlEncodeUTF8(entry.getKey()), urlEncodeUTF8(entry.getValue())));
        }
        return sb.toString();
    }

    public static HttpURLConnection getConnection(String urlString, String method, String api_key, String urlParams) throws IOException {
        if (method == "GET" && urlParams!= null && ! urlParams.isEmpty()) {
            urlString = urlString+"?"+urlParams;
        }
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(90000);
        connection.setReadTimeout(90000);
        boolean isDoOutput = isPostOrPut(method) ? true : false;
        connection.setDoOutput(isDoOutput);

        if(api_key !=null && !api_key.trim().isEmpty()){
            connection.addRequestProperty("Authorization", "Token token="+Base64.encodeToString(api_key.getBytes(), Base64.DEFAULT));
        }

        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");

        if (isPostOrPut(method)  && urlParams!= null && ! urlParams.isEmpty()) {
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParams);
            wr.flush();
            wr.close();
        }
        return connection;
    }


    public static boolean isPostOrPut(String method) {
        return method.equals("POST") || method.equals("PUT") || method.equals("PATCH");

    }

    public static JSONObject convertToJsonObject(HttpURLConnection connection) throws IOException, JSONException {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuffer response = new StringBuffer();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);

        }
        in.close();

        JSONObject responseJson = new JSONObject(response.toString());
        return responseJson;
    }

}