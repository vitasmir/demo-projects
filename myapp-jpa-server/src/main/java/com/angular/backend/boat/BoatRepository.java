package com.angular.backend.boat;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BoatRepository extends JpaRepository<Boat, Long> {
    boolean existsByNameIgnoreCase(String name);
}
