package com.speedline.user.service;

import com.speedline.user.dto.ScheduleTemplateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ScheduleTemplateService {
    Page<ScheduleTemplateDTO.Response> getAllTemplates(Pageable pageable, String search, Boolean isActive);
    List<ScheduleTemplateDTO.Response> getActiveTemplates();
    ScheduleTemplateDTO.Response getTemplate(Long id);
    ScheduleTemplateDTO.Response createTemplate(ScheduleTemplateDTO.CreateRequest request);
    ScheduleTemplateDTO.Response updateTemplate(Long id, ScheduleTemplateDTO.CreateRequest request);
    void deleteTemplate(Long id);
    void toggleActive(Long id);
}
