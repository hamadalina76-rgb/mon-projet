package com.speedline.delivery.dispatch.config.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "dispatch_replay_pair_score")
@Getter
@Setter
public class DispatchReplayPairScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "replay_session_id", nullable = false)
    private DispatchReplaySessionEntity replaySession;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "courier_id", nullable = false)
    private Long courierId;

    @Column(name = "total_cost")
    private Double totalCost;

    @Column(nullable = false)
    private boolean eliminated;

    @Column(name = "elimination_reason")
    private String eliminationReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "components_json", nullable = false, columnDefinition = "jsonb")
    private String componentsJson = "[]";
}
