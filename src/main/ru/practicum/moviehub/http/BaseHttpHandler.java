package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.api.ErrorResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {

        byte[] responseBody = json.getBytes(StandardCharsets.UTF_8);

        ex.getResponseHeaders().set("Content-Type", CT_JSON);

        ex.sendResponseHeaders(status, responseBody.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(responseBody);
        }
        ex.close();
    }

    protected void sendNoContent(HttpExchange ex) throws java.io.IOException {

        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(204, -1);
        ex.close();
    }

    protected void sendError(HttpExchange ex, int status, ErrorResponse error) throws IOException {
        String json = new Gson().toJson(error);
        sendJson(ex, status, json);
    }
}