package com.hotel.billing.repository;

import com.hotel.billing.entity.Folio;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hotel.common.enums.FolioStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

@Repository
public interface FolioRepository extends JpaRepository<Folio, Long> {

    Optional<Folio> findByStayId(Long stayId);

    boolean existsByStayId(Long stayId);

    Page<Folio> findByStatus(FolioStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Folio f WHERE f.folioId = :id")
    Optional<Folio> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Folio f WHERE f.stayId = :stayId")
    Optional<Folio> findByStayIdWithLock(@Param("stayId") Long stayId);
}
