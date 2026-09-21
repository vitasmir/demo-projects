package com.angular.backend.boat;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class BoatRentalDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoatResponse {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationResponse {
        private Long id;
        private Long boatId;
        private String reservedBy;
        private String reservedByUserId;
        private String startDateTime;
        private String endDateTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettingsResponse {
        private String dayStartTime;
        private String dayEndTime;
        private Integer slotDurationMinutes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleResponse {
        private String date;
        private SettingsResponse settings;
        private List<BoatResponse> boats;
        private List<ReservationResponse> reservations;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdminOverviewResponse {
        private SettingsResponse settings;
        private List<BoatResponse> boats;
        private String suggestedBoatName;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateReservationRequest {
        @NotNull
        private Long boatId;

        @NotBlank
        private String date;

        @NotBlank
        private String startTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateBoatRequest {
        @NotBlank
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateSettingsRequest {
        @NotBlank
        private String dayStartTime;

        @NotBlank
        private String dayEndTime;
    }
}
