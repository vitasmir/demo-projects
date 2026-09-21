package cz.listek.backend.settings;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
public class ProductInterestSettingsController {

    private final ProductInterestSettingsRepository repository;

    public ProductInterestSettingsController(ProductInterestSettingsRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/interest")
    public ProductInterestSettings settings() {
        return repository.findById(true).orElseThrow();
    }
}
