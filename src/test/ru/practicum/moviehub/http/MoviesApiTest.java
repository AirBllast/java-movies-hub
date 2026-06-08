package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore moviesStore;
    Gson gson = new Gson();

    @BeforeAll
    static void beforeAll() {
        moviesStore = new MoviesStore();
        server = new MoviesServer(moviesStore, 8080);
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void beforeEach() {
        moviesStore.clear();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");


        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void postMovie_whenAllIsCorrect() throws Exception {
        Movie newMovie = new Movie("Карты, деньги, два ствола", 1998);

        String json = gson.toJson(newMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
        Movie savedMovie = gson.fromJson(resp.body(), Movie.class);
        assertNotNull(savedMovie.getId(), "Сервер должен присвоить и вернуть ID фильма");
        assertEquals("Карты, деньги, два ствола", savedMovie.getTitle());
        assertEquals(1998, savedMovie.getReleaseYear());
        assertEquals(1, moviesStore.getMovies().size(), "В хранилище должен появиться 1 фильм");

    }

    @Test
    void postMovie_whenTitleIsEmpty() throws Exception {
        Movie newMovie = new Movie("", 1998);

        String json = gson.toJson(newMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        assertEquals(422, resp.statusCode(), "Заголовок фильма не должен быть пустым");
    }

    @Test
    void postMovie_whenYearIsNotCorrect() throws Exception {
        Movie newMovie = new Movie("Карты, деньги, два ствола", 1880);

        String json = gson.toJson(newMovie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        assertEquals(422, resp.statusCode(), "Год выпуска фильма должен быть от 1888 до 2027");
    }

    @Test
    void getMovie_whenUseID() throws Exception {

        Movie movie = new Movie("Карты, деньги, два ствола", 1998);
        Movie savedMovie = moviesStore.saveMovie(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + savedMovie.getId()))
                .GET()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(200, resp.statusCode(), "GET /movies/{id} должен вернуть 200 OK");

        Movie returnedMovie = gson.fromJson(resp.body(), Movie.class);
        assertEquals(savedMovie.getId(), returnedMovie.getId());
        assertEquals("Карты, деньги, два ствола", returnedMovie.getTitle());

    }

    @Test
    void deleteMovie_whenUseID() throws Exception {

        Movie movie = new Movie("Карты, деньги, два ствола", 1998);
        Movie savedMovie = moviesStore.saveMovie(movie);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + savedMovie.getId()))
                .DELETE()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(204, resp.statusCode(), "DELETE /movies/{id} должен вернуть 204 OK");

    }

    @Test
    void getMovies_whenUseFilterByYear() throws Exception {
        moviesStore.saveMovie(new Movie("Карты, деньги, два ствола", 1998));
        moviesStore.saveMovie(new Movie("Бойцовский клуб", 1999));
        moviesStore.saveMovie(new Movie("Матрица", 1999));
        moviesStore.saveMovie(new Movie("Зелёная миля", 1999));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?releaseYear=1999"))
                .GET()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(200, resp.statusCode());
        Movie[] returnedMovies = gson.fromJson(resp.body(), Movie[].class);
        assertEquals(3, returnedMovies.length, "Должно вернуться ровно 3 фильма");
        assertEquals(1999, returnedMovies[0].getReleaseYear());
        assertEquals(1999, returnedMovies[1].getReleaseYear());
        assertEquals(1999, returnedMovies[2].getReleaseYear());
    }

    @Test
    void getMovies_whenUseFilterByYearNotFound() throws Exception {
        moviesStore.saveMovie(new Movie("Карты, деньги, два ствола", 1998));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?releaseYear=1999"))
                .GET()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(200, resp.statusCode());
        assertEquals("[]", resp.body().trim(), "Должен вернуться пустой массив");
    }

    @Test
    void getMovies_whenUseFilterByYearWhenYearNotNumber() throws Exception {
        moviesStore.saveMovie(new Movie("Карты, деньги, два ствола", 1998));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?releaseYear=wrong"))
                .GET()
                .build();

        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        assertEquals(400, resp.statusCode());
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);
        assertEquals("Некорректный формат параметра year", error.getError());
    }
}