package org.example.bookingservice;

import org.example.bookingservice.exceptions.MovieNotFoundException;
import org.example.bookingservice.exceptions.MovieServiceException;
import org.example.bookingservice.models.constants.BookingStatus;
import org.example.bookingservice.models.dto.requests.CreateBookingDetailRequest;
import org.example.bookingservice.models.dto.requests.CreateBookingRequest;
import org.example.bookingservice.models.dto.responses.BookingResponse;
import org.example.bookingservice.models.dto.responses.MovieResponse;
import org.example.bookingservice.models.entities.Booking;
import org.example.bookingservice.models.entities.BookingDetail;
import org.example.bookingservice.models.repositories.BookingDetailRepository;
import org.example.bookingservice.models.repositories.BookingRepository;
import org.example.bookingservice.models.services.impl.BookingServiceImpl;
import org.example.bookingservice.models.services.impl.MovieGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BookingServiceImplTest {

    private BookingRepository bookingRepository;
    private BookingDetailRepository bookingDetailRepository;
    private MovieGatewayService movieGatewayService;
    private KafkaTemplate<String, String> kafkaTemplate;
    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        bookingRepository = mock(BookingRepository.class);
        bookingDetailRepository = mock(BookingDetailRepository.class);
        movieGatewayService = mock(MovieGatewayService.class);
        //noinspection unchecked
        kafkaTemplate = mock(KafkaTemplate.class);
        bookingService = new BookingServiceImpl(bookingRepository, bookingDetailRepository, movieGatewayService, kafkaTemplate);
    }

    @Test
    void createBooking_success() {
        CreateBookingRequest request = new CreateBookingRequest(
                "Nguyen Van A",
                "nguyenvana@example.com",
                List.of(
                        new CreateBookingDetailRequest(1L, 2),
                        new CreateBookingDetailRequest(2L, 1)
                )
        );

        when(movieGatewayService.getMovieById(1L))
                .thenReturn(new MovieResponse(1L, "Movie 1", 120000.0));
        when(movieGatewayService.getMovieById(2L))
                .thenReturn(new MovieResponse(2L, "Movie 2", 90000.0));

        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            b.setId(10L);
            return b;
        });

        when(bookingDetailRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<BookingDetail> details = invocation.getArgument(0);
            long id = 100L;
            for (BookingDetail detail : details) {
                detail.setId(id++);
            }
            return details;
        });

        BookingResponse response = bookingService.createBooking(request);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Nguyen Van A", response.customerName());
        assertEquals("nguyenvana@example.com", response.customerEmail());
        assertEquals(330000.0, response.total());
        assertEquals(BookingStatus.PENDING, response.status());
        assertEquals(2, response.items().size());

        assertEquals(1L, response.items().get(0).movieId());
        assertEquals("Movie 1", response.items().get(0).movieTitle());
        assertEquals(2, response.items().get(0).quantity());
        assertEquals(120000.0, response.items().get(0).unitPrice());
        assertEquals(240000.0, response.items().get(0).subtotal());

        assertEquals(2L, response.items().get(1).movieId());
        assertEquals("Movie 2", response.items().get(1).movieTitle());
        assertEquals(1, response.items().get(1).quantity());
        assertEquals(90000.0, response.items().get(1).unitPrice());
        assertEquals(90000.0, response.items().get(1).subtotal());

        verify(kafkaTemplate).send("booking-created", "nguyenvana@example.com");
    }

    @Test
    void createBooking_movieNotFound_doesNotSaveOrSendKafka() {
        CreateBookingRequest request = new CreateBookingRequest(
                "Nguyen Van A",
                "nguyenvana@example.com",
                List.of(
                        new CreateBookingDetailRequest(1L, 2),
                        new CreateBookingDetailRequest(99L, 1)
                )
        );

        when(movieGatewayService.getMovieById(1L))
                .thenReturn(new MovieResponse(1L, "Movie 1", 120000.0));
        when(movieGatewayService.getMovieById(99L))
                .thenThrow(new MovieNotFoundException(99L));

        assertThrows(MovieNotFoundException.class, () -> bookingService.createBooking(request));

        verify(bookingRepository, never()).save(any());
        verify(bookingDetailRepository, never()).saveAll(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void createBooking_movieServiceUnavailable_doesNotSaveOrSendKafka() {
        CreateBookingRequest request = new CreateBookingRequest(
                "Nguyen Van A",
                "nguyenvana@example.com",
                List.of(
                        new CreateBookingDetailRequest(1L, 2)
                )
        );

        when(movieGatewayService.getMovieById(1L))
                .thenThrow(new MovieServiceException("Movie service is unavailable for movie 1"));

        assertThrows(MovieServiceException.class, () -> bookingService.createBooking(request));

        verify(bookingRepository, never()).save(any());
        verify(bookingDetailRepository, never()).saveAll(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }
}
