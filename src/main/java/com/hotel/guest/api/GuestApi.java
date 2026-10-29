package com.hotel.guest.api;

import java.util.List;

/**
 * Cổng giao tiếp công khai của module Guest dành cho các module khác (Booking, Frontdesk).
 * Chỉ truyền nhận các record DTO và kiểu dữ liệu cơ bản, tuyệt đối không lộ Entity.
 */
public interface GuestApi {

    GuestInfoDto getGuest(Long guestId);

    GuestInfoDto findOrCreateGuest(GuestCreateCommand command);

    void addLoyaltyPoints(Long guestId, Integer points);

    List<Long> findGuestIdsByKeyword(String keyword);
}
