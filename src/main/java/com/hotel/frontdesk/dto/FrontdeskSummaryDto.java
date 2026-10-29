package com.hotel.frontdesk.dto;

public record FrontdeskSummaryDto(
        long inHouseCount,
        long expectedArrivalsToday,
        long expectedDeparturesToday,
        long cleanAvailableRooms
) {
}
