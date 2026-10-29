package com.hotel.guest.repository;

import com.hotel.common.enums.GuestIdType;
import com.hotel.guest.entity.Guest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {

    Optional<Guest> findByPhone(String phone);

    Optional<Guest> findByIdNumber(String idNumber);

    Optional<Guest> findByIdTypeAndIdNumber(GuestIdType idType, String idNumber);

    Optional<Guest> findByIdTypeAndIdNumberAndGuestIdNot(GuestIdType idType, String idNumber, Long guestId);

    @Query("SELECT g FROM Guest g WHERE " +
           "(:keyword IS NULL OR LOWER(g.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "                  OR g.phone LIKE CONCAT('%', :keyword, '%') " +
           "                  OR g.idNumber LIKE CONCAT('%', :keyword, '%') " +
           "                  OR LOWER(g.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:idNumber IS NULL OR g.idNumber = :idNumber) AND " +
           "(:phone IS NULL OR g.phone = :phone) AND " +
           "(:email IS NULL OR LOWER(g.email) = LOWER(:email))")
    Page<Guest> searchGuests(
            @Param("keyword") String keyword,
            @Param("idNumber") String idNumber,
            @Param("phone") String phone,
            @Param("email") String email,
            Pageable pageable
    );

    @Query("SELECT g.guestId FROM Guest g WHERE " +
           "LOWER(g.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR g.phone LIKE CONCAT('%', :keyword, '%') " +
           "OR g.idNumber LIKE CONCAT('%', :keyword, '%') " +
           "OR LOWER(g.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Long> findGuestIdsByKeyword(@Param("keyword") String keyword);
}
