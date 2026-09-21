package cz.listek.admin.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import cz.listek.admin.api.AdminDtos.AccountResponse;
import cz.listek.admin.api.AdminDtos.AdminProfileResponse;
import cz.listek.admin.api.AdminDtos.AdminUserResponse;
import cz.listek.admin.api.AdminDtos.ApplicationResponse;
import cz.listek.admin.api.AdminDtos.CreateOverdraftRequest;
import cz.listek.admin.api.AdminDtos.DashboardResponse;
import cz.listek.admin.api.AdminDtos.DecisionRequest;
import cz.listek.admin.api.AdminDtos.InterestSettingsResponse;
import cz.listek.admin.api.AdminDtos.LoanReportResponse;
import cz.listek.admin.api.AdminDtos.UpdateAdminProfileRequest;
import cz.listek.admin.api.AdminDtos.UpdateInterestSettingsRequest;
import cz.listek.admin.service.AdminAuthService;
import cz.listek.admin.service.AdminWorkflowService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminWorkflowService workflowService;
    private final AdminAuthService authService;

    public AdminController(AdminWorkflowService workflowService, AdminAuthService authService) {
        this.workflowService = workflowService;
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    public AdminDtos.AuthResponse login(@Valid @RequestBody AdminDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PatchMapping("/auth/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestBody AdminDtos.PasswordRequest request,
            @org.springframework.web.bind.annotation.RequestHeader("X-Admin-Session") String session) {
        authService.changePassword(session, request.password());
    }

    @GetMapping("/auth/profile")
    public AdminProfileResponse profile(@org.springframework.web.bind.annotation.RequestHeader("X-Admin-Session") String session) {
        return authService.profile(session);
    }

    @PatchMapping("/auth/profile")
    public AdminProfileResponse updateProfile(@Valid @RequestBody UpdateAdminProfileRequest request,
            @org.springframework.web.bind.annotation.RequestHeader("X-Admin-Session") String session) {
        return authService.updateProfile(session, request);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse createAdmin(@Valid @RequestBody AdminDtos.CreateAdminRequest request,
            @org.springframework.web.bind.annotation.RequestHeader("X-Admin-Session") String session) {
        return authService.createAdmin(session, request);
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users(
            @org.springframework.web.bind.annotation.RequestHeader("X-Admin-Session") String session) {
        return authService.listAdmins(session);
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return workflowService.dashboard();
    }

    @GetMapping("/accounts")
    public List<AccountResponse> accounts() {
        return workflowService.accounts();
    }

    @GetMapping("/loans")
    public List<ApplicationResponse> loans() {
        return workflowService.loans();
    }

    @GetMapping("/reports/loans")
    public List<LoanReportResponse> loanReport(
            @org.springframework.web.bind.annotation.RequestHeader("X-Admin-Session") String session) {
        authService.usernameForSession(session);
        return workflowService.loanReport();
    }

    @PatchMapping("/loans/{id}/decision")
    public ApplicationResponse decideLoan(@PathVariable UUID id, @Valid @RequestBody DecisionRequest request,
            @org.springframework.web.bind.annotation.RequestHeader("X-Admin-Session") String session) {
        return workflowService.decideLoan(id, request, authService.usernameForSession(session));
    }

    @GetMapping("/overdrafts")
    public List<ApplicationResponse> overdrafts() {
        return workflowService.overdrafts();
    }

    @PostMapping("/overdrafts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse createOverdraft(@Valid @RequestBody CreateOverdraftRequest request) {
        return workflowService.createOverdraft(request);
    }

    @PatchMapping("/overdrafts/{id}/decision")
    public ApplicationResponse decideOverdraft(@PathVariable UUID id, @Valid @RequestBody DecisionRequest request) {
        return workflowService.decideOverdraft(id, request);
    }

    @DeleteMapping("/overdrafts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void terminateOverdraft(@PathVariable UUID id) {
        workflowService.terminateOverdraft(id);
    }

    @GetMapping("/settings/interest")
    public InterestSettingsResponse interestSettings() {
        return workflowService.interestSettings();
    }

    @PatchMapping("/settings/interest")
    public InterestSettingsResponse updateInterestSettings(@Valid @RequestBody UpdateInterestSettingsRequest request) {
        return workflowService.updateInterestSettings(request);
    }
}
