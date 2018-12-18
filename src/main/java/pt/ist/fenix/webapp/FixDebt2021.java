package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.events.EventExemption;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustificationType;
import org.fenixedu.academic.ui.spring.service.AccountingManagementService;
import org.fenixedu.bennu.core.domain.User;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.EventProcessor;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Set;
import java.util.stream.Collectors;

public class FixDebt2021 extends SapCustomTask {

    @Override
    protected void runTask(final ErrorLogConsumer errorLogConsumer, final EventLogger elogger) {
        final User responsible = User.findByUsername("ist24616");

        final Event originEvent = FenixFramework.getDomainObject("571166225859298");
        final Event destinEvent = FenixFramework.getDomainObject("1415591155990990");

        originEvent.getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT)
                .forEach(this::fixDebt);

        originEvent.getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT)
                .forEach(sr -> sr.delete());

        EventExemption eventExemption = new EventExemption(originEvent, responsible.getPerson(), originEvent.getOriginalAmountToPay(),
                EventExemptionJustificationType.CANCELLED, new DateTime(), "Dívida criada no ano lectivo errado");
        eventExemption.setWhenCreated(originEvent.getWhenOccured().plusSeconds(1));
        forceCalculate(originEvent, errorLogConsumer, elogger);

        final AccountingTransaction tx = new AccountingManagementService().depositAdvancement(destinEvent, originEvent, responsible);
        forceCalculate(destinEvent, errorLogConsumer, elogger);
    }

    private void fixDebt(final SapRequest debt) {
        String request = debt.getRequest();
        request = request.replace("2020-09-01", "2019-09-01");
        request = request.replace("2021-08-31", "2020-08-31");
//        request = request.replace("2020/2021", "2019/2020");
        debt.setRequest(request);
    }

    private void forceCalculate(final Event event, final ErrorLogConsumer errorLogConsumer, final EventLogger elogger) {
        final Set<SapRequest> notSent = event.getSapRequestSet().stream().filter(sr -> !sr.getSent()).collect(Collectors.toSet());
        final Set<SapRequest> noIntegrated = event.getSapRequestSet().stream().filter(sr -> !sr.getIntegrated()).collect(Collectors.toSet());
        final Set<SapRequest> notIgnored = event.getSapRequestSet().stream().filter(sr -> !sr.getIgnore() && sr.getAnulledRequest() != null).collect(Collectors.toSet());
        try {
            notSent.forEach(sr -> sr.setSent(true));
            noIntegrated.forEach(sr -> sr.setIntegrated(true));
            notIgnored.forEach(sr -> sr.setIgnore(true));
            EventProcessor.registerEventSapRequests(errorLogConsumer, elogger, event, false);
        } finally {
            notSent.forEach(sr -> sr.setSent(false));
            noIntegrated.forEach(sr -> sr.setIntegrated(false));
            notIgnored.forEach(sr -> sr.setIgnore(false));
        }
    }
}
