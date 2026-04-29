package com.speedline.support.service.impl;

import com.speedline.support.domain.SupportTicket;
import com.speedline.support.domain.SupportTicket.UserType;
import com.speedline.support.domain.TicketCategory;
import com.speedline.support.domain.TicketPriority;
import com.speedline.support.domain.TicketStatus;
import com.speedline.support.repository.SupportTicketRepository;
import com.speedline.support.repository.TicketMessageRepository;
import com.speedline.support.service.SupportTicketService.MessageDTO;
import com.speedline.support.service.SupportTicketService.TicketDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceImplTest {

    @Mock
    private SupportTicketRepository supportTicketRepository;

    @Mock
    private TicketMessageRepository ticketMessageRepository;

    @InjectMocks
    private SupportTicketServiceImpl supportTicketService;

    // -------------------------------------------------------------------------
    // createTicket (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("createTicket")
    class CreateTicket {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> supportTicketService.createTicket(
                    1L, UserType.CUSTOMER, TicketCategory.ORDER,
                    TicketPriority.HIGH, "Order issue",
                    "My order is missing", 100L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getTicketById (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getTicketById")
    class GetTicketById {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> supportTicketService.getTicketById(1L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getTicketByNumber (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getTicketByNumber")
    class GetTicketByNumber {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> supportTicketService.getTicketByNumber("TKT-2026-000001"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getUserTickets (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getUserTickets")
    class GetUserTickets {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            Pageable pageable = PageRequest.of(0, 10);
            assertThatThrownBy(() -> supportTicketService.getUserTickets(1L, pageable))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getTicketsByStatus (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getTicketsByStatus")
    class GetTicketsByStatus {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            Pageable pageable = PageRequest.of(0, 10);
            assertThatThrownBy(() -> supportTicketService.getTicketsByStatus(TicketStatus.OPEN, pageable))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // updateStatus (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> supportTicketService.updateStatus(1L, TicketStatus.IN_PROGRESS))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // assignTicket (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("assignTicket")
    class AssignTicket {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> supportTicketService.assignTicket(1L, 5L, "Agent Smith"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // addMessage (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("addMessage")
    class AddMessage {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> supportTicketService.addMessage(
                    1L, 1L, "USER", "John", "Help please", null))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // resolveTicket (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("resolveTicket")
    class ResolveTicket {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> supportTicketService.resolveTicket(1L, "Issue resolved"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // reopenTicket (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("reopenTicket")
    class ReopenTicket {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> supportTicketService.reopenTicket(1L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // -------------------------------------------------------------------------
    // getTicketMessages (TODO - throws UnsupportedOperationException)
    // -------------------------------------------------------------------------
    @Nested
    @DisplayName("getTicketMessages")
    class GetTicketMessages {

        @Test
        @DisplayName("should throw UnsupportedOperationException because not yet implemented")
        void shouldThrowUnsupportedOperationException() {
            assertThatThrownBy(() -> supportTicketService.getTicketMessages(1L))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
