package com.speedline.review.service.impl;

import com.speedline.review.domain.Review;
import com.speedline.review.domain.Review.TargetType;
import com.speedline.review.repository.RatingRepository;
import com.speedline.review.repository.ReviewRepository;
import com.speedline.review.service.ReviewService.ReviewDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private RatingRepository ratingRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Review sampleReview;

    @BeforeEach
    void setUp() {
        sampleReview = Review.builder()
                .id("rev-001")
                .orderId(100L)
                .customerId(1L)
                .customerName("John Doe")
                .targetType(TargetType.PARTNER)
                .targetId(10L)
                .rating(4)
                .comment("Great food!")
                .imageUrls(List.of("https://img.example.com/1.jpg"))
                .response(null)
                .responseDate(null)
                .isVerified(true)
                .isVisible(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // createReview (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("createReview")
    class CreateReview {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> reviewService.createReview(
                    100L, 1L, TargetType.PARTNER, 10L, 5, "Excellent", null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getReviewById (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getReviewById")
    class GetReviewById {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> reviewService.getReviewById("rev-001"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getPartnerReviews (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getPartnerReviews")
    class GetPartnerReviews {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            Pageable pageable = PageRequest.of(0, 10);
            assertThatThrownBy(() -> reviewService.getPartnerReviews(10L, pageable))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getCustomerReviews (implemented)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getCustomerReviews")
    class GetCustomerReviews {

        @Test
        @DisplayName("should return paged reviews mapped to DTOs")
        void shouldReturnPagedReviews() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> page = new PageImpl<>(List.of(sampleReview), pageable, 1);
            when(reviewRepository.findByCustomerId(1L, pageable)).thenReturn(page);

            Page<ReviewDTO> result = reviewService.getCustomerReviews(1L, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            ReviewDTO dto = result.getContent().get(0);
            assertThat(dto.id()).isEqualTo("rev-001");
            assertThat(dto.orderId()).isEqualTo(100L);
            assertThat(dto.customerId()).isEqualTo(1L);
            assertThat(dto.customerName()).isEqualTo("John Doe");
            assertThat(dto.targetType()).isEqualTo(TargetType.PARTNER);
            assertThat(dto.targetId()).isEqualTo(10L);
            assertThat(dto.rating()).isEqualTo(4);
            assertThat(dto.comment()).isEqualTo("Great food!");
            assertThat(dto.isVerified()).isTrue();
        }

        @Test
        @DisplayName("should return empty page when customer has no reviews")
        void shouldReturnEmptyPageWhenNoReviews() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(reviewRepository.findByCustomerId(99L, pageable)).thenReturn(emptyPage);

            Page<ReviewDTO> result = reviewService.getCustomerReviews(99L, pageable);

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("should map review with null response fields correctly")
        void shouldMapReviewWithNullResponseFields() {
            Review reviewNoResponse = Review.builder()
                    .id("rev-002")
                    .orderId(200L)
                    .customerId(1L)
                    .customerName("Jane")
                    .targetType(TargetType.COURIER)
                    .targetId(20L)
                    .rating(3)
                    .comment("OK")
                    .imageUrls(null)
                    .response(null)
                    .responseDate(null)
                    .isVerified(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Review> page = new PageImpl<>(List.of(reviewNoResponse), pageable, 1);
            when(reviewRepository.findByCustomerId(1L, pageable)).thenReturn(page);

            Page<ReviewDTO> result = reviewService.getCustomerReviews(1L, pageable);

            ReviewDTO dto = result.getContent().get(0);
            assertThat(dto.response()).isNull();
            assertThat(dto.responseDate()).isNull();
            assertThat(dto.imageUrls()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // getAverageRating (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getAverageRating")
    class GetAverageRating {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> reviewService.getAverageRating(TargetType.PARTNER, 10L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // respondToReview (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("respondToReview")
    class RespondToReview {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> reviewService.respondToReview("rev-001", 5L, "Thank you!"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // deleteReview (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("deleteReview")
    class DeleteReview {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> reviewService.deleteReview("rev-001"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getCourierReviews (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getCourierReviews")
    class GetCourierReviews {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            Pageable pageable = PageRequest.of(0, 10);
            assertThatThrownBy(() -> reviewService.getCourierReviews(5L, pageable))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getOrderReview (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getOrderReview")
    class GetOrderReview {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> reviewService.getOrderReview(100L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
