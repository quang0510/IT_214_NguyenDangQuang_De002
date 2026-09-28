package org.example.bookingservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.bookingservice.models.constants.BookingStatus;
import org.example.bookingservice.models.dto.requests.CreateBookingDetailRequest;
import org.example.bookingservice.models.dto.requests.CreateBookingRequest;
import org.example.bookingservice.models.dto.responses.BookingDetailResponse;
import org.example.bookingservice.models.dto.responses.BookingResponse;
import org.example.bookingservice.models.dto.responses.MovieResponse;
import org.example.bookingservice.models.entities.Booking;
import org.example.bookingservice.models.entities.BookingDetail;
import org.example.bookingservice.models.repositories.BookingDetailRepository;
import org.example.bookingservice.models.repositories.BookingRepository;
import org.example.bookingservice.models.services.BookingService;
import org.jspecify.annotations.NonNull;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

        private static final String DEFAULT_BOOKING_CREATED_TOPIC = "booking-created";

        private final BookingRepository bookingRepository;
        private final BookingDetailRepository bookingDetailRepository;
        private final MovieGatewayService movieGatewayService;
        private final KafkaTemplate<String, String> kafkaTemplate;

        @Value("${booking.kafka.booking-created-topic:booking-created}")
        private String bookingCreatedTopic = DEFAULT_BOOKING_CREATED_TOPIC;

        @Override
        @Transactional
        public BookingResponse createBooking(CreateBookingRequest request) {
                List<ResolvedItem> resolvedItems = request.items().stream()
                                .map(this::resolveMovie)
                                .toList();

                double total = resolvedItems.stream()
                                .mapToDouble(item -> item.movie().ticketPrice() * item.request().quantity())
                                .sum();

                Booking booking = bookingRepository.save(Booking.builder()
                                .customerName(request.customerName().trim())
                                .customerEmail(request.customerEmail().trim())
                                .total(total)
                                .status(BookingStatus.PENDING)
                                .build());

                List<BookingDetail> details = resolvedItems.stream()
                                .map(item -> BookingDetail.builder()
                                                .booking(booking)
                                                .movieId(item.movie().id())
                                                .quantity(item.request().quantity())
                                                .unitPrice(item.movie().ticketPrice())
                                                .build())
                                .toList();

                List<BookingDetail> savedDetails = bookingDetailRepository.saveAll(details);

                kafkaTemplate.send(bookingCreatedTopic, booking.getCustomerEmail());

                List<BookingDetailResponse> itemResponses = getBookingDetailResponses(savedDetails, resolvedItems);

                return new BookingResponse(
                                booking.getId(),
                                booking.getCustomerName(),
                                booking.getCustomerEmail(),
                                booking.getTotal(),
                                booking.getStatus(),
                                itemResponses);
        }

        private static @NonNull List<BookingDetailResponse> getBookingDetailResponses(List<BookingDetail> savedDetails,
                        List<ResolvedItem> resolvedItems) {
                List<BookingDetailResponse> itemResponses = new ArrayList<>(savedDetails.size());

                for (int index = 0; index < savedDetails.size(); index++) {
                        BookingDetail detail = savedDetails.get(index);
                        MovieResponse movie = resolvedItems.get(index).movie();
                        itemResponses.add(new BookingDetailResponse(
                                        detail.getId(),
                                        detail.getMovieId(),
                                        movie.title(),
                                        detail.getQuantity(),
                                        detail.getUnitPrice(),
                                        detail.getUnitPrice() * detail.getQuantity()));
                }
                return itemResponses;
        }

        private ResolvedItem resolveMovie(CreateBookingDetailRequest item) {
                MovieResponse movie = movieGatewayService.getMovieById(item.movieId());
                return new ResolvedItem(item, movie);
        }

        private record ResolvedItem(CreateBookingDetailRequest request, MovieResponse movie) {
        }
}
