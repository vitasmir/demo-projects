package cz.listek.backend.settings;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductInterestSettingsRepository extends JpaRepository<ProductInterestSettings, Boolean> {
}
