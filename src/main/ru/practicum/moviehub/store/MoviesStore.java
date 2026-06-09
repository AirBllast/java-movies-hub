package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MoviesStore {

    private final HashMap<Integer, Movie> movies = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    public MoviesStore() {
    }

    public Movie saveMovie(Movie movie) {
        movie.setId(idGenerator.getAndIncrement());
        movies.put(movie.getId(), movie);
        return movie;
    }

    public List<Movie> findMoviesByYear(int releaseYear) {

        return movies.values().stream()
                .filter(movie -> movie.getReleaseYear() == releaseYear)
                .toList();
    }

    public Movie getMovie(int id) {
        return movies.get(id);
    }

    public ArrayList<Movie> getMovies() {
        return new ArrayList<>(movies.values());
    }

    public void deleteMovie(int id) {
        movies.remove(id);
    }

    public void clear() {
        movies.clear();
        idGenerator.set(1);
    }

}