package org.example.bookingservice;

import org.example.bookingservice.controllers.BookingController;
import org.example.bookingservice.exceptions.GlobalExceptionHandler;
import org.example.bookingservice.exceptions.MovieNotFoundException;
import org.example.bookingservice.exceptions.MovieServiceException;
import org.example.bookingservice.models.constants.BookingStatus;
import org.example.bookingservice.models.dto.requests.CreateBookingDetailRequest;
import org.example.bookingservice.models.dto.requests.CreateBookingRequest;
import org.example.bookingservice.models.dto.responses.BookingDetailResponse;
import org.example.bookingservice.models.dto.responses.BookingResponse;
import org.example.bookingservice.models.services.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @Test
    void createBooking_validRequest_returns201Created() throws Exception {
        BookingResponse response = new BookingResponse(
                1L,
                "Nguyen Van A",
                "nguyenvana@example.com",
                210000.0,
                BookingStatus.PENDING,
                List.of(new BookingDetailResponse(1L, 1L, "Movie 1", 2, 105000.0, 210000.0))
        );
        when(bookingService.createBooking(any())).thenReturn(response);

        String json = """
                {
                    "customerName": "Nguyen Van A",
                    "customerEmail": "nguyenvana@example.com",
                    "items": [
                        {
                            "movieId": 1,
                            "quantity": 2
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.customerName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.customerEmail").value("nguyenvana@example.com"))
                .andExpect(jsonPath("$.total").value(210000.0))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createBooking_invalidRequest_returns400BadRequest() throws Exception {
        String json = """
                {
                    "customerName": "",
                    "customerEmail": "invalid-email",
                    "items": []
                }
                """;

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    void createBooking_movieNotFound_returns404NotFound() throws Exception {
        when(bookingService.createBooking(any())).thenThrow(new MovieNotFoundException(99L));

        String json = """
                {
                    "customerName": "Nguyen Van A",
                    "customerEmail": "nguyenvana@example.com",
                    "items": [
                        {
                            "movieId": 99,
                            "quantity": 1
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Movie not found"));
    }

    @Test
    void createBooking_movieServiceUnavailable_returns502BadGateway() throws Exception {
        when(bookingService.createBooking(any())).thenThrow(new MovieServiceException("Movie service is unavailable"));

        String json = """
                {
                    "customerName": "Nguyen Van A",
                    "customerEmail": "nguyenvana@example.com",
                    "items": [
                        {
                            "movieId": 1,
                            "quantity": 1
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/v1/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Movie service error"));
    }
}
