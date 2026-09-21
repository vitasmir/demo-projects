package com.angular.backend.boat;

import java.time.Instant;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "boat_rental_settings")
@Getter
@Setter
@NoArgsConstructor
public class BoatRentalSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "day_start_time", nullable = false)
    private LocalTime dayStartTime;

    @Column(name = "day_end_time", nullable = false)
    private LocalTime dayEndTime;

    @Column(name = "slot_duration_minutes", nullable = false)
    private Integer slotDurationMinutes;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
