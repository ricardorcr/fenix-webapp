package pt.ist.fenix.webapp;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.events.AnnualEvent;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;

public class CheckSapRequestDifference extends CustomTask {

    private final DateTime when = new DateTime(2017, 12, 31, 23, 59, 59, 999);

    @Override
    public void runTask() throws Exception {
        //Event.paymentsPredicate = (t, when) -> !t.getWhenProcessed().isAfter(when);

        Bennu.getInstance().getAccountingEventsSet().stream()
                .filter(e -> e.getWhenOccured().isBefore(when) && executionYearOf(e).getQualifiedName().equals("2017/2018"))
                .filter(e -> !e.getSapRequestSet().isEmpty()).filter(this::check).map(Event::getExternalId)
                .forEach(eventID -> taskLog("Bad boy: %s\n", eventID));
    }

    private boolean check(Event event) {
        Set<SapRequest> srSet = event.getSapRequestSet().stream().filter(sr -> sr.getRequest().length() > 2)
                .filter(sr -> sr.getRequestType().equals(SapRequestType.INVOICE)).collect(Collectors.toSet());

        if (srSet.isEmpty()) {
            return false;
        }
        Money requestValue = srSet.stream().map(SapRequest::getValue).reduce(Money.ZERO, Money::add);
        DebtInterestCalculator calculator = event.getDebtInterestCalculator(when);
        Money eventDueAmount = new Money(calculator.getDueAmount());

        BigDecimal originalAmount = calculator.getDebtAmount();
        //Money shouldBe = new Money(originalAmount.subtract(requestValue.getAmount()));

        if (!requestValue.equals(eventDueAmount)) {
            taskLog("Due amount: %s\n", eventDueAmount);
            taskLog("Request amount: %s\n", requestValue);
            //  taskLog("Should be: %s\n", shouldBe);
            taskLog("Original Amount: %s\n", originalAmount);
            taskLog("Real Original Amount: %s\n", event.getOriginalAmountToPay());
        }

        return !requestValue.equals(eventDueAmount);
    }

    private ExecutionYear executionYearOf(final Event event) {
        return event instanceof AnnualEvent ? ((AnnualEvent) event).getExecutionYear() : ExecutionYear
                .readByDateTime(event.getWhenOccured());
    }
}
