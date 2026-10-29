package com.hotel.billing.entity;

import com.hotel.common.enums.FolioStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "folio")
public class Folio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folio_id")
    private Long folioId;

    @Column(name = "stay_id", nullable = false, unique = true)
    private Long stayId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private FolioStatus status = FolioStatus.OPEN;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount = 0L;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    public Folio() {
    }

    public Long getFolioId() {
        return folioId;
    }

    public void setFolioId(Long folioId) {
        this.folioId = folioId;
    }

    public Long getStayId() {
        return stayId;
    }

    public void setStayId(Long stayId) {
        this.stayId = stayId;
    }

    public FolioStatus getStatus() {
        return status;
    }

    public void setStatus(FolioStatus status) {
        this.status = status;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }
}
