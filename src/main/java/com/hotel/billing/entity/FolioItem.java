package com.hotel.billing.entity;

import com.hotel.common.enums.FolioItemType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "folio_item")
public class FolioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_id")
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folio_id", nullable = false)
    private Folio folio;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 10)
    private FolioItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private HotelService service;

    @Column(name = "description", length = 300)
    private String description;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false)
    private Long unitPrice;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "posted_at", nullable = false)
    private LocalDateTime postedAt = LocalDateTime.now();

    @Column(name = "posted_by")
    private Long postedBy;

    public FolioItem() {
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Folio getFolio() {
        return folio;
    }

    public void setFolio(Folio folio) {
        this.folio = folio;
    }

    public FolioItemType getItemType() {
        return itemType;
    }

    public void setItemType(FolioItemType itemType) {
        this.itemType = itemType;
    }

    public HotelService getService() {
        return service;
    }

    public void setService(HotelService service) {
        this.service = service;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Long getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Long unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(LocalDateTime postedAt) {
        this.postedAt = postedAt;
    }

    public Long getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(Long postedBy) {
        this.postedBy = postedBy;
    }
}
