package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;
    Gson gson = new Gson();

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    public void methodGet(HttpExchange ex, String[] pathParts) throws IOException {

        if (pathParts.length == 2 && pathParts[1].equals("movies")) {
            String query = ex.getRequestURI().getQuery();

            if (query == null || query.isEmpty()) {
                String json = gson.toJson(moviesStore.getMovies());
                sendJson(ex, 200, json);
                return;
            }
            if (query.startsWith("releaseYear=")) {
                try {
                    int releaseYear = Integer.parseInt(query.split("=")[1]);
                    List<Movie> filteredMovies = moviesStore.getMovies().stream()
                            .filter(movie -> movie.getReleaseYear() == releaseYear)
                            .toList();
                    sendJson(ex, 200, gson.toJson(filteredMovies));
                } catch (NumberFormatException e) {
                    ErrorResponse errorResponse = new ErrorResponse("Некорректный формат параметра year");
                    sendError(ex, 400, errorResponse);
                }
                return;
            }
        }
        if (pathParts.length == 3 && pathParts[1].equals("movies")) {
            try {
                int id = Integer.parseInt(pathParts[2]);
                Movie movie = moviesStore.getMovie(id);
                if (movie != null) {
                    sendJson(ex, 200, gson.toJson(movie));
                } else {
                    ErrorResponse errorResponse = new ErrorResponse("Фильм с id=" + id + " не найден");
                    sendError(ex, 404, errorResponse);
                }
            } catch (NumberFormatException e) {
                ErrorResponse errorResponse = new ErrorResponse("Некорректный формат ID");
                sendError(ex, 400, errorResponse);
            }
            return;
        }
        ex.sendResponseHeaders(404, -1);
        ex.close();
    }

    public void methodPost(HttpExchange ex) throws IOException {

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Movie newMovie = gson.fromJson(body, Movie.class);

        if (newMovie.getTitle() == null || newMovie.getTitle().isBlank()) {
            ErrorResponse errorResponse = new ErrorResponse("Название фильма не должно быть пустым");
            sendError(ex, 422, errorResponse);
            return;
        }
        if (newMovie.getReleaseYear() < 1888 || newMovie.getReleaseYear() > 2027) {
            ErrorResponse errorResponse = new ErrorResponse("Год выпуска должен быть от 1888 до 2027");
            sendError(ex, 422, errorResponse);
            return;
        }
        Movie savedMovie = moviesStore.saveMovie(newMovie);
        String json = gson.toJson(savedMovie);
        sendJson(ex, 201, json);

    }

    public void methodDelete(HttpExchange ex, String[] pathParts) throws IOException {

        if (pathParts.length == 3 && pathParts[1].equals("movies")) {
            try {
                int id = Integer.parseInt(pathParts[2]);
                Movie movie = moviesStore.getMovie(id);
                if (movie != null) {
                    moviesStore.deleteMovie(id);
                    sendNoContent(ex);
                } else {
                    ErrorResponse errorResponse = new ErrorResponse("Фильм с id=" + id + " не найден");
                    sendError(ex, 404, errorResponse);
                }
            } catch (NumberFormatException e) {
                ErrorResponse errorResponse = new ErrorResponse("Некорректный формат ID");
                sendError(ex, 400, errorResponse);
            }
        } else {
            ex.sendResponseHeaders(404, -1);
            ex.close();
        }
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {

        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String[] pathParts = path.split("/");

        if (method.equalsIgnoreCase("GET")) {
            methodGet(ex, pathParts);

        } else if (method.equalsIgnoreCase("POST")) {
            methodPost(ex);

        } else if (method.equalsIgnoreCase("DELETE")) {
            methodDelete(ex, pathParts);

        } else {
            ex.sendResponseHeaders(405, -1);
            ex.close();
        }
    }
}
