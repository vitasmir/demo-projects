package com.angular.backend.boat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.angular.backend.boat.BoatRentalDtos.AdminOverviewResponse;
import com.angular.backend.boat.BoatRentalDtos.BoatResponse;
import com.angular.backend.boat.BoatRentalDtos.CreateBoatRequest;
import com.angular.backend.boat.BoatRentalDtos.CreateReservationRequest;
import com.angular.backend.boat.BoatRentalDtos.ReservationResponse;
import com.angular.backend.boat.BoatRentalDtos.ScheduleResponse;
import com.angular.backend.boat.BoatRentalDtos.SettingsResponse;
import com.angular.backend.boat.BoatRentalDtos.UpdateSettingsRequest;

@Service
@Transactional
public class BoatRentalService {

    private static final Pattern DEFAULT_NAME_PATTERN = Pattern.compile("^Lodka\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);
    private static final Logger log = LoggerFactory.getLogger(BoatRentalService.class);
    private static final LocalTime STANDARD_DAY_START_TIME = LocalTime.of(8, 0);
    private static final LocalTime STANDARD_DAY_END_TIME = LocalTime.of(20, 0);
    private static final int DEFAULT_SLOT_DURATION_MINUTES = 30;
        private static final Set<String> ADMIN_ROLE_NAMES = Set.of(
            "admin",
            "role_admin",
            "task-management-admin",
            "task_management_admin",
            "task.admin",
            "tm_admin");

    private final BoatRepository boatRepository;
    private final BoatReservationRepository boatReservationRepository;
    private final BoatRentalSettingRepository boatRentalSettingRepository;

    public BoatRentalService(
            BoatRepository boatRepository,
            BoatReservationRepository boatReservationRepository,
            BoatRentalSettingRepository boatRentalSettingRepository) {
        this.boatRepository = boatRepository;
        this.boatReservationRepository = boatReservationRepository;
        this.boatRentalSettingRepository = boatRentalSettingRepository;
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(String dateValue) {
        LocalDate date = parseDate(dateValue);
        try {
            BoatRentalSetting settings = getSettingsEntity();
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);

            List<BoatResponse> boats = boatRepository.findAll().stream()
                    .sorted(Comparator.comparing(Boat::getId))
                    .map(this::toBoatResponse)
                    .toList();

            List<ReservationResponse> reservations = boatReservationRepository
                    .findByStartDateTimeGreaterThanEqualAndStartDateTimeLessThanOrderByStartDateTimeAsc(dayStart, dayEnd)
                    .stream()
                    .map(this::toReservationResponse)
                    .toList();

            return new ScheduleResponse(date.toString(), toSettingsResponse(settings), boats, reservations);
        } catch (DataAccessException ex) {
            log.warn("Unable to load rental schedule from database, returning default schedule: {}", ex.getMessage());
            return new ScheduleResponse(date.toString(), toSettingsResponse(createDefaultSettings()), Collections.emptyList(), Collections.emptyList());
        }
    }

    public ReservationResponse createReservation(Jwt jwt, CreateReservationRequest request) {
        Boat boat = boatRepository.findById(request.getBoatId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lodka nebyla nalezena."));
        String username = extractUsername(jwt);

        BoatRentalSetting settings = getSettingsEntity();
        LocalDate date = parseDate(request.getDate());
        LocalTime startTime = parseTime(request.getStartTime());
        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        LocalDateTime endDateTime = startDateTime.plusMinutes(settings.getSlotDurationMinutes());

        validateTimeSlot(settings, startTime, endDateTime.toLocalTime());
        validateSlotAlignment(settings, startTime);

        if (boatReservationRepository.existsOverlapping(boat.getId(), startDateTime, endDateTime)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vybraný termín je již obsazený.");
        }

        BoatReservation reservation = new BoatReservation();
        reservation.setBoat(boat);
        reservation.setReservedBy(username);
        reservation.setReservedByUserId(username);
        reservation.setStartDateTime(startDateTime);
        reservation.setEndDateTime(endDateTime);
        reservation.setCreatedAt(Instant.now());

        return toReservationResponse(boatReservationRepository.save(reservation));
    }

    public void cancelReservation(Jwt jwt, Long reservationId) {
        BoatReservation reservation = boatReservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rezervace nebyla nalezena."));
        String username = extractUsername(jwt);

        boolean reservationOwner = Objects.equals(reservation.getReservedByUserId(), username);
        if (!reservationOwner && !hasAdminRole(jwt)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Rezervaci může zrušit jen její autor nebo admin.");
        }

        boatReservationRepository.delete(reservation);
    }

    @Transactional(readOnly = true)
    public AdminOverviewResponse getAdminOverview() {
        try {
            BoatRentalSetting settings = getSettingsEntity();
            List<BoatResponse> boats = boatRepository.findAll().stream()
                    .sorted(Comparator.comparing(Boat::getId))
                    .map(this::toBoatResponse)
                    .toList();
            return new AdminOverviewResponse(toSettingsResponse(settings), boats, buildSuggestedBoatName(boats));
        } catch (DataAccessException ex) {
            log.warn("Unable to load admin overview from database, returning default overview: {}", ex.getMessage());
            return new AdminOverviewResponse(toSettingsResponse(createDefaultSettings()), Collections.emptyList(), "Lodka 1");
        }
    }

    public BoatResponse createBoat(CreateBoatRequest request) {
        String normalizedName = request.getName().trim();
        if (boatRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lodka s tímto názvem už existuje.");
        }

        Boat boat = new Boat();
        boat.setName(normalizedName);
        boat.setCreatedAt(Instant.now());
        return toBoatResponse(boatRepository.save(boat));
    }

    public SettingsResponse updateSettings(UpdateSettingsRequest request) {
        LocalTime dayStartTime = parseTime(request.getDayStartTime());
        LocalTime dayEndTime = parseTime(request.getDayEndTime());

        if (!dayEndTime.isAfter(dayStartTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Koncový čas musí být později než počáteční čas.");
        }

        long minutes = Duration.between(dayStartTime, dayEndTime).toMinutes();
        if (minutes < 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Časové okno musí být alespoň 30 minut dlouhé.");
        }
        if (minutes % 30 != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Časové okno musí být dělitelné po 30 minutách.");
        }

        BoatRentalSetting settings = getSettingsEntity();
        settings.setDayStartTime(dayStartTime);
        settings.setDayEndTime(dayEndTime);
        settings.setUpdatedAt(Instant.now());
        return toSettingsResponse(boatRentalSettingRepository.save(settings));
    }

    private void validateTimeSlot(BoatRentalSetting settings, LocalTime startTime, LocalTime endTime) {
        if (startTime.isBefore(settings.getDayStartTime()) || endTime.isAfter(settings.getDayEndTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rezervace musí být uvnitř nastaveného časového okna.");
        }
    }

    private void validateSlotAlignment(BoatRentalSetting settings, LocalTime startTime) {
        long minutesFromStart = Duration.between(settings.getDayStartTime(), startTime).toMinutes();
        if (minutesFromStart < 0 || minutesFromStart % settings.getSlotDurationMinutes() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rezervaci lze vytvořit jen na 30minutových slotech.");
        }
    }

    private BoatRentalSetting getSettingsEntity() {
        return boatRentalSettingRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefaultSettings);
    }

    private BoatRentalSetting createDefaultSettings() {
        BoatRentalSetting defaultSettings = new BoatRentalSetting();
        defaultSettings.setDayStartTime(STANDARD_DAY_START_TIME);
        defaultSettings.setDayEndTime(STANDARD_DAY_END_TIME);
        defaultSettings.setSlotDurationMinutes(DEFAULT_SLOT_DURATION_MINUTES);
        defaultSettings.setUpdatedAt(Instant.now());
        return defaultSettings;
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datum musí být ve formátu YYYY-MM-DD.");
        }
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Čas musí být ve formátu HH:mm.");
        }
    }

    private BoatResponse toBoatResponse(Boat boat) {
        return new BoatResponse(boat.getId(), boat.getName());
    }

    private ReservationResponse toReservationResponse(BoatReservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getBoat().getId(),
                reservation.getReservedBy(),
                reservation.getReservedByUserId(),
                reservation.getStartDateTime().toString(),
                reservation.getEndDateTime().toString());
    }

    private String extractUsername(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Pro vytvoření nebo zrušení rezervace musíte být přihlášen(a).");
        }

        String preferredUsername = jwt.getClaimAsString("preferred_username");
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            return preferredUsername.trim();
        }

        String subject = jwt.getSubject();
        if (subject != null && !subject.isBlank()) {
            return subject.trim();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Přihlášený uživatel nemá dostupné uživatelské jméno.");
    }

    private boolean hasAdminRole(Jwt jwt) {
        if (jwt == null) {
            return false;
        }

        Set<String> normalizedRoles = new LinkedHashSet<>();
        collectRoleValuesFromClaim(jwt.getClaim("realm_access"), normalizedRoles);

        Object resourceAccessClaim = jwt.getClaim("resource_access");
        if (resourceAccessClaim instanceof Map<?, ?> resourceAccess) {
            for (Object clientAccess : resourceAccess.values()) {
                collectRoleValuesFromClaim(clientAccess, normalizedRoles);
            }
        }

        return normalizedRoles.stream().anyMatch(ADMIN_ROLE_NAMES::contains);
    }

    private void collectRoleValuesFromClaim(Object claimValue, Set<String> roles) {
        if (!(claimValue instanceof Map<?, ?> claimMap)) {
            return;
        }

        Object rawRoles = claimMap.get("roles");
        if (!(rawRoles instanceof Iterable<?> iterableRoles)) {
            return;
        }

        for (Object rawRole : iterableRoles) {
            if (rawRole instanceof String role && !role.isBlank()) {
                roles.add(role.trim().toLowerCase());
            }
        }
    }

    private SettingsResponse toSettingsResponse(BoatRentalSetting settings) {
        return new SettingsResponse(
                settings.getDayStartTime().toString(),
                settings.getDayEndTime().toString(),
                settings.getSlotDurationMinutes());
    }

    private String buildSuggestedBoatName(List<BoatResponse> boats) {
        int nextIndex = boats.stream()
                .map(BoatResponse::getName)
                .map(DEFAULT_NAME_PATTERN::matcher)
                .filter(Matcher::matches)
                .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
                .max()
                .orElse(boats.size()) + 1;
        return "Lodka " + nextIndex;
    }
}
