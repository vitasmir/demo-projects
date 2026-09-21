package cz.listek.backend.payment;

import java.time.YearMonth;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "standing_order_execution",
        uniqueConstraints = @UniqueConstraint(columnNames = {"standing_order_id", "execution_month"})
)
public class StandingOrderExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "standing_order_id", nullable = false)
    private StandingOrder standingOrder;

    @Column(name = "execution_month", nullable = false, length = 7)
    private String executionMonth;

    protected StandingOrderExecution() {
    }

    public StandingOrderExecution(StandingOrder standingOrder, YearMonth executionMonth) {
        this.standingOrder = standingOrder;
        this.executionMonth = executionMonth.toString();
    }

    public UUID getId() {
        return id;
    }

    public StandingOrder getStandingOrder() {
        return standingOrder;
    }

    public String getExecutionMonth() {
        return executionMonth;
    }
}
