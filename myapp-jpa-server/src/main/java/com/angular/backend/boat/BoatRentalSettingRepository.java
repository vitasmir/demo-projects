package com.angular.backend.boat;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BoatRentalSettingRepository extends JpaRepository<BoatRentalSetting, Long> {
    Optional<BoatRentalSetting> findFirstByOrderByIdAsc();
}
