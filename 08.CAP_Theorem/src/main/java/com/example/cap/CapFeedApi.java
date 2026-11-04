package com.example.cap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Standalone, in-memory API bonus. The distributed Cassandra design is in README.
 * Compile and run with JDK 17+. This demo does not implement authentication.
 */
public final class CapFeedApi {
    private static final int MAX_BODY_BYTES = 8192;
    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<Post>> POSTS =
            new ConcurrentHashMap<>();

    private CapFeedApi() {}

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8080), 0);
        var executor = Executors.newFixedThreadPool(8);
        server.setExecutor(executor);
        server.createContext("/", CapFeedApi::handle);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(1);
            executor.shutdown();
        }));
        server.start();
        System.out.println("Feed demo API: http://127.0.0.1:8080");
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } catch (BadRequest ex) {
            send(exchange, ex.status, "{\"error\":" + quote(ex.getMessage()) + "}");
        } catch (RuntimeException ex) {
            System.err.println("Request failed: " + ex.getClass().getSimpleName());
            send(exchange, 500, "{\"error\":\"Internal server error\"}");
        } finally {
            exchange.close();
        }
    }

    private static void route(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/health".equals(path)) {
            requireMethod(exchange, "GET");
            send(exchange, 200, "{\"status\":\"UP\",\"storage\":\"in-memory\"}");
        } else if ("/posts".equals(path)) {
            requireMethod(exchange, "POST");
            createPost(exchange);
        } else if (path.startsWith("/feed/")) {
            requireMethod(exchange, "GET");
            String userId = path.substring("/feed/".length());
            validateUserId(userId);
            var queue = POSTS.get(userId);
            List<Post> posts = queue == null ? List.of() : queue.stream()
                    .sorted(Comparator.comparing(Post::createdAt).reversed()
                            .thenComparing(Post::id))
                    .limit(50)
                    .toList();
            String body = posts.stream().map(Post::toJson)
                    .collect(Collectors.joining(",", "[", "]"));
            send(exchange, 200, body);
        } else {
            throw new BadRequest(404, "Route not found");
        }
    }

    private static void createPost(HttpExchange exchange) throws IOException {
        String type = exchange.getRequestHeaders().getFirst("Content-Type");
        if (type == null || !"application/x-www-form-urlencoded".equalsIgnoreCase(
                type.split(";", 2)[0].trim())) {
            throw new BadRequest(415, "Use application/x-www-form-urlencoded");
        }

        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) {
            throw new BadRequest(413, "Body exceeds 8192 bytes");
        }
        Map<String, String> form = parseForm(new String(bytes, StandardCharsets.UTF_8));
        String userId = form.get("userId");
        String content = form.get("content");
        validateUserId(userId);
        if (content == null || content.isBlank()
                || content.codePointCount(0, content.length()) > 280) {
            throw new BadRequest(400, "content must contain 1 to 280 characters");
        }

        Post post = new Post(UUID.randomUUID().toString(), userId, content, Instant.now());
        POSTS.computeIfAbsent(userId, ignored -> new ConcurrentLinkedQueue<>()).add(post);
        send(exchange, 201, post.toJson());
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> fields = new HashMap<>();
        try {
            for (String part : body.split("&")) {
                String[] pair = part.split("=", 2);
                if (pair.length != 2) {
                    throw new BadRequest(400, "Malformed form field");
                }
                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                if (fields.putIfAbsent(key, value) != null) {
                    throw new BadRequest(400, "Duplicate form field");
                }
            }
        } catch (IllegalArgumentException ex) {
            throw new BadRequest(400, "Invalid form encoding");
        }
        return fields;
    }

    private static void validateUserId(String userId) {
        if (userId == null || !userId.matches("[A-Za-z0-9_-]{1,64}")) {
            throw new BadRequest(400, "userId must match [A-Za-z0-9_-]{1,64}");
        }
    }

    private static void requireMethod(HttpExchange exchange, String method) {
        if (!method.equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", method);
            throw new BadRequest(405, "Method not allowed");
        }
    }

    private static void send(HttpExchange exchange, int status, String json)
            throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> result.append("\\\"");
                case '\\' -> result.append("\\\\");
                default -> {
                    if (character < 0x20) {
                        result.append(String.format("\\u%04x", (int) character));
                    } else {
                        result.append(character);
                    }
                }
            }
        }
        return result.append('"').toString();
    }

    private record Post(String id, String userId, String content, Instant createdAt) {
        String toJson() {
            return "{\"id\":" + quote(id)
                    + ",\"userId\":" + quote(userId)
                    + ",\"content\":" + quote(content)
                    + ",\"createdAt\":" + quote(createdAt.toString()) + "}";
        }
    }

    private static final class BadRequest extends RuntimeException {
        final int status;

        BadRequest(int status, String message) {
            super(message);
            this.status = status;
        }
    }
}
