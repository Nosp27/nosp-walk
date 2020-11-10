package com.nosp.nospwalk.connectors;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpBuilder {
    private static final int JSON = 886;
    private static final int PLAIN = 945;

    private HttpURLConnection connection;

    private URL url;
    private String data = "";
    private String method = "GET";
    private Map<String, List<String>> headers;

    public HttpBuilder url(String link) {
        try {
            url = new URL(link);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        return this;
    }

    public HttpBuilder get() {
        method = "GET";
        return this;
    }

    public HttpBuilder post() {
        method = "POST";
        return this;
    }

    public HttpBuilder header(String key, String value) {
        if (headers == null)
            headers = new HashMap<>();
        if (!headers.containsKey(key) || headers.get(key) == null)
            headers.put(key, new ArrayList<String>());
        headers.get(key).add(value);
        return this;
    }

    public HttpBuilder plainData(String data) {
        if (method.equals("GET"))
            throw new IllegalStateException("Data used for POST requests only");
        this.header("Content-Type", "text/plain");
        this.data = data;
        return this;
    }

    public HttpBuilder jsonData(Map data) {
        if (method.equals("GET"))
            throw new IllegalStateException("Data used for POST requests only");
        this.header("Content-Type", "application/json");
        this.data = new JSONObject(data).toString();
        return this;
    }

    public Response request() throws IOException {
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            for (Map.Entry<String, List<String>> headerEntry : headers.entrySet())
                for (String value : headerEntry.getValue())
                    connection.setRequestProperty(headerEntry.getKey(), value);
            IOUtils.write(data, connection.getOutputStream(), StandardCharsets.UTF_8);

            int responseCode = connection.getResponseCode();
            String data = IOUtils.toString(connection.getInputStream(), StandardCharsets.UTF_8);
            return new Response(responseCode, connection.getResponseMessage(), data);
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    public static class Response {
        public int code;
        public String data;
        public String responseMessage;

        private Response(int responseCode, String responseMessage, String data) {
            code = responseCode;
            this.data = data;
            this.responseMessage = responseMessage;
        }

        public Response raiseForStatus() throws IOException {
            if (code != 200) {
                throw new IOException(responseMessage);
            }
            return this;
        }

        public JSONTokener json() {
            return new JSONTokener(data);
        }
    }
}
