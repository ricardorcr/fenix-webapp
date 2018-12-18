package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.events.AnnualEvent;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

import pt.ist.fenixedu.domain.SapRequest;

public class DebtSentReport extends CustomTask {
    private final DateTime when = new DateTime(2017, 12, 31, 23, 59, 59, 999);

    @Override
    public void runTask() throws Exception {

        Money totalSent = Bennu.getInstance().getAccountingEventsSet().stream()
                .filter(e -> e.getWhenOccured().isBefore(when) && executionYearOf(e).getQualifiedName().equals("2017/2018"))
                .filter(e -> !e.getSapRequestSet().isEmpty()).flatMap(e -> e.getSapRequestSet().stream())
                .filter(sr -> sr.getIntegrated()).filter(sr -> sr.getRequest().length() > 2).map(SapRequest::getValue)
                .reduce(Money.ZERO, Money::add);

        taskLog("Enviado para o SAP: %s\n", totalSent);

    }

    private ExecutionYear executionYearOf(final Event event) {
        return event instanceof AnnualEvent ? ((AnnualEvent) event).getExecutionYear() : ExecutionYear
                .readByDateTime(event.getWhenOccured());
    }

}
