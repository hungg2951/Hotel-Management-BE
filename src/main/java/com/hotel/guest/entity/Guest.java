package com.hotel.guest.entity;

import com.hotel.common.enums.GuestGender;
import com.hotel.common.enums.GuestIdType;
import com.hotel.common.enums.GuestLoyaltyTier;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "guest")
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guest_id")
    private Long guestId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    private GuestGender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "nationality", length = 50)
    private String nationality = "Viet Nam";

    @Enumerated(EnumType.STRING)
    @Column(name = "id_type", length = 10)
    private GuestIdType idType;

    @Column(name = "id_number", length = 30)
    private String idNumber;

    @Column(name = "address", length = 500)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "loyalty_tier", nullable = false, length = 10)
    private GuestLoyaltyTier loyaltyTier = GuestLoyaltyTier.NONE;

    @Column(name = "loyalty_points", nullable = false)
    private Integer loyaltyPoints = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Guest() {
    }

    public Long getGuestId() {
        return guestId;
    }

    public void setGuestId(Long guestId) {
        this.guestId = guestId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public GuestGender getGender() {
        return gender;
    }

    public void setGender(GuestGender gender) {
        this.gender = gender;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public GuestIdType getIdType() {
        return idType;
    }

    public void setIdType(GuestIdType idType) {
        this.idType = idType;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public GuestLoyaltyTier getLoyaltyTier() {
        return loyaltyTier;
    }

    public void setLoyaltyTier(GuestLoyaltyTier loyaltyTier) {
        this.loyaltyTier = loyaltyTier;
    }

    public Integer getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(Integer loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
