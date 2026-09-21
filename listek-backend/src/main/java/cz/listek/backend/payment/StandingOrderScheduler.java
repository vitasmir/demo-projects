package cz.listek.backend.payment;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import cz.listek.backend.account.AccountService;

@Component
public class StandingOrderScheduler {

    private final StandingOrderRepository standingOrderRepository;
    private final StandingOrderExecutionRepository executionRepository;
    private final AccountService accountService;
    private final Clock clock;

    public StandingOrderScheduler(
            StandingOrderRepository standingOrderRepository,
            StandingOrderExecutionRepository executionRepository,
            AccountService accountService) {
        this.standingOrderRepository = standingOrderRepository;
        this.executionRepository = executionRepository;
        this.accountService = accountService;
        this.clock = Clock.system(ZoneId.of("Europe/Prague"));
    }

    @Scheduled(cron = "${app.scheduler.standing-orders-cron:0 0 1 * * *}")
    @Transactional
    public void executeDueStandingOrders() {
        LocalDate today = LocalDate.now(clock);
        YearMonth executionMonth = YearMonth.from(today);
        standingOrderRepository.findByActiveTrue().forEach(order -> executeIfDue(order, today, executionMonth));
    }

    void executeIfDue(StandingOrder order, LocalDate today, YearMonth executionMonth) {
        var lockedOrder = standingOrderRepository.findWithLockById(order.getId()).orElse(null);
        if (lockedOrder == null || !lockedOrder.isActive() || lockedOrder.getDayOfMonth() > today.getDayOfMonth()) {
            return;
        }

        executionRepository.saveAndFlush(new StandingOrderExecution(lockedOrder, executionMonth));
        accountService.transfer(
                lockedOrder.getAccount().getId(),
                lockedOrder.getTargetAccountNumber(),
                lockedOrder.getAmount(),
                lockedOrder.getDescription(), lockedOrder.getVariableSymbol(), lockedOrder.getSpecificSymbol());
    }
}
