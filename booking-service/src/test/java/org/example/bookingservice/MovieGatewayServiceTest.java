package org.example.bookingservice;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.example.bookingservice.clients.MovieClient;
import org.example.bookingservice.exceptions.MovieNotFoundException;
import org.example.bookingservice.exceptions.MovieServiceException;
import org.example.bookingservice.models.dto.responses.MovieResponse;
import org.example.bookingservice.models.services.impl.MovieGatewayService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
class MovieGatewayServiceTest {

    @MockitoBean
    private MovieClient movieClient;

    @Autowired
    private MovieGatewayService movieGatewayService;

    @Test
    void whenMovieExists_returnsMovie() {
        MovieResponse expected = new MovieResponse(1L, "Movie 1", 100.0);
        when(movieClient.getMovieById(1L)).thenReturn(expected);

        MovieResponse result = movieGatewayService.getMovieById(1L);
        assertEquals(expected, result);
    }

    @Test
    void whenMovieNotFound_throwsMovieNotFoundException() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/v1/movies/99", Collections.emptyMap(), null, null, new RequestTemplate());
        when(movieClient.getMovieById(99L)).thenThrow(new FeignException.NotFound("Not Found", request, null, null));

        assertThrows(MovieNotFoundException.class, () -> movieGatewayService.getMovieById(99L));
    }

    @Test
    void whenMovieServiceFails_throwsMovieServiceException() {
        when(movieClient.getMovieById(1L)).thenThrow(new RuntimeException("Connection timed out"));

        MovieServiceException ex = assertThrows(MovieServiceException.class, () -> movieGatewayService.getMovieById(1L));
        assertTrue(ex.getMessage().contains("Movie service is unavailable"));
    }
}
