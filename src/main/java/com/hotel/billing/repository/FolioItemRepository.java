package com.hotel.billing.repository;

import com.hotel.billing.entity.FolioItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolioItemRepository extends JpaRepository<FolioItem, Long> {

    List<FolioItem> findByFolioFolioId(Long folioId);

    List<FolioItem> findByFolioFolioIdOrderByPostedAtAsc(Long folioId);

    Optional<FolioItem> findByItemIdAndFolioFolioId(Long itemId, Long folioId);

    @Query("SELECT COALESCE(SUM(fi.amount), 0) FROM FolioItem fi WHERE fi.folio.folioId = :folioId")
    Long calculateTotalAmountByFolioId(@Param("folioId") Long folioId);

    @Query("SELECT COALESCE(SUM(fi.amount), 0) FROM FolioItem fi WHERE fi.postedAt >= :start AND fi.postedAt < :end")
    Long calculateTotalRecognizedBetween(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);

    @Query("SELECT COALESCE(SUM(fi.amount), 0) FROM FolioItem fi WHERE fi.itemType = :itemType AND fi.postedAt >= :start AND fi.postedAt < :end")
    Long calculateTotalByTypeBetween(
            @Param("itemType") com.hotel.common.enums.FolioItemType itemType,
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end
    );

    List<FolioItem> findByPostedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
