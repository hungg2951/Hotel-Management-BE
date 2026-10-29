package com.hotel.billing.repository;

import com.hotel.billing.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByInvoiceNo(String invoiceNo);

    Optional<Invoice> findByFolioFolioId(Long folioId);

    boolean existsByFolioFolioId(Long folioId);

    boolean existsByInvoiceNo(String invoiceNo);
}
