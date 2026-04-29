package com.speedline.analytics.service.impl;

import com.speedline.analytics.domain.Report.ReportType;
import com.speedline.analytics.repository.ReportRepository;
import com.speedline.analytics.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ReportServiceImpl.
 *
 * The report service methods are currently stubs that throw
 * UnsupportedOperationException. These tests verify the current behavior
 * and serve as a scaffold for when the implementations are completed.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    // ===================== generateReport =====================

    @Nested
    @DisplayName("generateReport")
    class GenerateReport {

        @Test
        @DisplayName("should throw UnsupportedOperationException for REVENUE report")
        void shouldThrowForRevenueReport() {
            ReportService.ReportParameters params = new ReportService.ReportParameters(
                    LocalDate.now().minusDays(30),
                    LocalDate.now(),
                    null, null, null, "DAY"
            );

            assertThatThrownBy(() ->
                    reportService.generateReport(ReportType.REVENUE, params, 1L, "PDF"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should throw UnsupportedOperationException for ORDERS report")
        void shouldThrowForOrdersReport() {
            ReportService.ReportParameters params = new ReportService.ReportParameters(
                    LocalDate.now().minusDays(7),
                    LocalDate.now(),
                    null, null, List.of("COMPLETED"), "WEEK"
            );

            assertThatThrownBy(() ->
                    reportService.generateReport(ReportType.ORDERS, params, 1L, "CSV"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should throw UnsupportedOperationException for COURIERS report")
        void shouldThrowForCouriersReport() {
            ReportService.ReportParameters params = new ReportService.ReportParameters(
                    LocalDate.now().minusDays(90),
                    LocalDate.now(),
                    null, 5L, null, "MONTH"
            );

            assertThatThrownBy(() ->
                    reportService.generateReport(ReportType.COURIERS, params, 2L, "XLSX"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ===================== getReportById =====================

    @Nested
    @DisplayName("getReportById")
    class GetReportById {

        @Test
        @DisplayName("should throw UnsupportedOperationException (not yet implemented)")
        void shouldThrowNotImplemented() {
            assertThatThrownBy(() -> reportService.getReportById(1L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ===================== downloadReport =====================

    @Nested
    @DisplayName("downloadReport")
    class DownloadReport {

        @Test
        @DisplayName("should throw UnsupportedOperationException (not yet implemented)")
        void shouldThrowNotImplemented() {
            assertThatThrownBy(() -> reportService.downloadReport(1L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ===================== getUserReports =====================

    @Nested
    @DisplayName("getUserReports")
    class GetUserReports {

        @Test
        @DisplayName("should throw UnsupportedOperationException (not yet implemented)")
        void shouldThrowNotImplemented() {
            assertThatThrownBy(() ->
                    reportService.getUserReports(1L, PageRequest.of(0, 10)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ===================== getAllReports =====================

    @Nested
    @DisplayName("getAllReports")
    class GetAllReports {

        @Test
        @DisplayName("should throw UnsupportedOperationException (not yet implemented)")
        void shouldThrowNotImplemented() {
            assertThatThrownBy(() ->
                    reportService.getAllReports(PageRequest.of(0, 10)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ===================== deleteReport =====================

    @Nested
    @DisplayName("deleteReport")
    class DeleteReport {

        @Test
        @DisplayName("should throw UnsupportedOperationException (not yet implemented)")
        void shouldThrowNotImplemented() {
            assertThatThrownBy(() -> reportService.deleteReport(1L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ===================== cleanupExpiredReports =====================

    @Nested
    @DisplayName("cleanupExpiredReports")
    class CleanupExpiredReports {

        @Test
        @DisplayName("should throw UnsupportedOperationException (not yet implemented)")
        void shouldThrowNotImplemented() {
            assertThatThrownBy(() -> reportService.cleanupExpiredReports())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
