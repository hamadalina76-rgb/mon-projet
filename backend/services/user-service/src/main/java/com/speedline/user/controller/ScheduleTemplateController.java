package com.speedline.user.controller;

import com.speedline.user.dto.ScheduleTemplateDTO;
import com.speedline.user.service.ScheduleTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD modèles de planning (réservé aux livreurs INTERNAL).
 * Base path après StripPrefix=1 : v1/admin/schedule-templates
 */
@RestController
@RequestMapping("v1/admin/schedule-templates")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ScheduleTemplateController {

    private final ScheduleTemplateService templateService;

    @GetMapping
    public ResponseEntity<Page<ScheduleTemplateDTO.Response>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive
    ) {
        log.info("GET schedule-templates - page: {}, size: {}, search: {}, isActive: {}", page, size, search, isActive);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(templateService.getAllTemplates(pageable, search, isActive));
    }

    @GetMapping("/active")
    public ResponseEntity<List<ScheduleTemplateDTO.Response>> getActive() {
        return ResponseEntity.ok(templateService.getActiveTemplates());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleTemplateDTO.Response> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.getTemplate(id));
    }

    @PostMapping
    public ResponseEntity<ScheduleTemplateDTO.Response> create(@RequestBody @Valid ScheduleTemplateDTO.CreateRequest req) {
        log.info("POST schedule-template: {}", req.getName());
        return ResponseEntity.ok(templateService.createTemplate(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduleTemplateDTO.Response> update(@PathVariable Long id,
                                                                @RequestBody @Valid ScheduleTemplateDTO.CreateRequest req) {
        log.info("PUT schedule-template {}", id);
        return ResponseEntity.ok(templateService.updateTemplate(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE schedule-template {}", id);
        templateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Void> toggle(@PathVariable Long id) {
        templateService.toggleActive(id);
        return ResponseEntity.ok().build();
    }
}
