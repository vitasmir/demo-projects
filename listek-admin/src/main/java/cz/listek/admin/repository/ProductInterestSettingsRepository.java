package cz.listek.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.listek.admin.domain.ProductInterestSettings;

public interface ProductInterestSettingsRepository extends JpaRepository<ProductInterestSettings, Boolean> {
}
