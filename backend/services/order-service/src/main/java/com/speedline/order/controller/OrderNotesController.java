package com.speedline.order.controller;

import com.speedline.order.domain.OrderInternalNote;
import com.speedline.order.repository.OrderInternalNoteRepository;
import com.speedline.order.repository.OrderRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders/{orderId}/notes")
@RequiredArgsConstructor
@Slf4j
public class OrderNotesController {

    private final OrderInternalNoteRepository noteRepository;
    private final OrderRepository orderRepository;

    @GetMapping
    public ResponseEntity<List<OrderInternalNote>> getNotes(@PathVariable Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable");
        }
        List<OrderInternalNote> notes = noteRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        return ResponseEntity.ok(notes);
    }

    @PostMapping
    public ResponseEntity<OrderInternalNote> addNote(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @RequestBody CreateNoteRequest request
    ) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Commande introuvable");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le contenu de la note est requis");
        }

        Long authorId = 0L;
        if (userId != null && !userId.isBlank()) {
            try { authorId = Long.parseLong(userId); } catch (NumberFormatException ignored) {}
        }

        OrderInternalNote note = OrderInternalNote.builder()
                .orderId(orderId)
                .content(request.getContent().trim())
                .visibility(request.getVisibility() != null ? request.getVisibility() : "ADMIN_ONLY")
                .authorId(authorId)
                .authorName(userName != null && !userName.isBlank() ? userName : "Admin")
                .authorRole("ADMIN")
                .build();

        note = noteRepository.save(note);
        log.info("Internal note added to order {}: noteId={}", orderId, note.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(note);
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable Long orderId,
            @PathVariable Long noteId,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    ) {
        OrderInternalNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note introuvable"));

        if (!note.getOrderId().equals(orderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La note n'appartient pas à cette commande");
        }

        // Only author can delete (super-admin bypass handled by frontend role check)
        Long authorId = 0L;
        if (userId != null && !userId.isBlank()) {
            try { authorId = Long.parseLong(userId); } catch (NumberFormatException ignored) {}
        }
        if (!note.getAuthorId().equals(authorId) && authorId != 0L) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seul l'auteur peut supprimer cette note");
        }

        noteRepository.delete(note);
        log.info("Internal note deleted from order {}: noteId={}", orderId, noteId);

        return ResponseEntity.noContent().build();
    }

    @Data
    public static class CreateNoteRequest {
        private String content;
        private String visibility;
    }
}
