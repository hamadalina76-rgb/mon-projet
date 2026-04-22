package com.speedline.delivery.dispatch.config.service;

import com.speedline.delivery.dispatch.config.persistence.DispatchConfigAuditEntity;
import com.speedline.delivery.dispatch.config.persistence.DispatchConfigAuditRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class DispatchConfigAuditCsvService {

    private static final DateTimeFormatter ISO_UTC = DateTimeFormatter.ISO_INSTANT;

    private final DispatchConfigAuditRepository auditRepository;
    private final MeterRegistry meterRegistry;

    public void export(Instant fromUtc, Instant toUtcExclusive, Writer writer) throws IOException {
        meterRegistry.counter("dispatch.config.audit_exports").increment();
        writer.write("occurred_at_utc,actor_user_id,actor_email,http_method,config_group,endpoint_path,diff_summary,config_version\n");
        int page = 0;
        Page<DispatchConfigAuditEntity> batch;
        do {
            batch = auditRepository.findByOccurredAtBetweenOrderByOccurredAtAsc(
                    fromUtc, toUtcExclusive, PageRequest.of(page++, 500));
            for (DispatchConfigAuditEntity row : batch.getContent()) {
                writer.write(String.join(",",
                        esc(ISO_UTC.format(row.getOccurredAt().atOffset(ZoneOffset.UTC))),
                        esc(row.getActorUserId()),
                        esc(row.getActorEmail()),
                        esc(row.getHttpMethod()),
                        esc(row.getConfigGroup()),
                        esc(row.getEndpointPath()),
                        esc(row.getDiffSummary()),
                        esc(String.valueOf(row.getConfigVersion()))
                ));
                writer.write('\n');
            }
        } while (batch.hasNext());
        writer.flush();
    }

    private static String esc(String v) {
        if (v == null) {
            return "";
        }
        String s = v.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
            return "\"" + s + "\"";
        }
        return s;
    }
}
