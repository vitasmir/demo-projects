package com.angular.backend.boat;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.angular.backend.boat.BoatRentalDtos.AdminOverviewResponse;
import com.angular.backend.boat.BoatRentalDtos.BoatResponse;
import com.angular.backend.boat.BoatRentalDtos.CreateBoatRequest;
import com.angular.backend.boat.BoatRentalDtos.CreateReservationRequest;
import com.angular.backend.boat.BoatRentalDtos.ReservationResponse;
import com.angular.backend.boat.BoatRentalDtos.ScheduleResponse;
import com.angular.backend.boat.BoatRentalDtos.SettingsResponse;
import com.angular.backend.boat.BoatRentalDtos.UpdateSettingsRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/boat-rental")
public class BoatRentalController {

    private final BoatRentalService boatRentalService;

    public BoatRentalController(BoatRentalService boatRentalService) {
        this.boatRentalService = boatRentalService;
    }

    @GetMapping("/schedule")
    public ScheduleResponse getSchedule(@RequestParam String date) {
        return boatRentalService.getSchedule(date);
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse createReservation(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateReservationRequest request) {
        return boatRentalService.createReservation(jwt, request);
    }

    @DeleteMapping("/reservations/{reservationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelReservation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reservationId) {
        boatRentalService.cancelReservation(jwt, reservationId);
    }

    @GetMapping("/admin/overview")
    public AdminOverviewResponse getAdminOverview() {
        return boatRentalService.getAdminOverview();
    }

    @PostMapping("/admin/boats")
    @ResponseStatus(HttpStatus.CREATED)
    public BoatResponse createBoat(@Valid @RequestBody CreateBoatRequest request) {
        return boatRentalService.createBoat(request);
    }

    @PutMapping("/admin/settings")
    public SettingsResponse updateSettings(@Valid @RequestBody UpdateSettingsRequest request) {
        return boatRentalService.updateSettings(request);
    }
}
