package pt.ist.fenix.webapp;

import java.util.Comparator;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtExemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.Payment;
import org.fenixedu.academic.domain.accounting.calculator.PaymentPlaceholder;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.joda.time.DateTime;

import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.FenixFramework;

public class InitCancelledEvents extends ReadCustomTask {

    private static final Comparator<? super Exemption> COMP = (e1, e2) -> e1.getWhenCreated().compareTo(e2.getWhenCreated());

    @Override
    public void runTask() throws Exception {
//        Bennu.getInstance().getAccountingEventsSet().stream()
//            .parallel().forEach(this::process);
        process(FenixFramework.getDomainObject("1692414683119638"));
        //process(FenixFramework.getDomainObject("3564824320596"));
    }

    private void fake(String oid) {
        FenixFramework.atomic(() -> {
            final Event event = FenixFramework.getDomainObject(oid);
            final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
            calculator.getAccountingEntries().stream()
                    .filter(e -> e instanceof DebtExemption)
                    .map(e -> (DebtExemption) e)
                    .forEach(e -> {
                        final Money exemptAmount = new Money(e.getAmount());
                        final SapEvent sapEvent = new SapEvent(event);
                        sapEvent.fakeSapRequest(SapRequestType.INVOICE, "ND0", exemptAmount, null);
                        sapEvent.fakeSapRequest(SapRequestType.CREDIT, "NA0", exemptAmount, e.getId());
                    });
        });
    }

    private void process(final Event event) {
        try {
            FenixFramework.atomic(() -> {
                if (event.isCancelled()) {
                    DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
                    final Money debtAmount = new Money(calculator.getDebtAmount());
                    if (debtAmount.isPositive()) {
                        event.exempt(User.findByUsername("ist24439").getPerson(), "Cancelamento de dívida");
                        if (calculator.getAccountingEntries().stream().anyMatch(e -> e instanceof Payment && !(e instanceof PaymentPlaceholder))) {
                            taskLog("Cancelled event contains payments %s%n", event.getExternalId());
                        } else if (event.getSapRequestSet().stream().filter(sr -> !sr.getIgnore()).count() == 0l) {
                            calculator = event.getDebtInterestCalculator(new DateTime().plusDays(1));
                            calculator.getAccountingEntries().stream()
                                    .filter(e -> e instanceof DebtExemption)
                                    .map(e -> (DebtExemption) e)
                                    .forEach(e -> {
                                        final Money exemptAmount = new Money(e.getAmount());
                                        final SapEvent sapEvent = new SapEvent(event);
                                        sapEvent.fakeSapRequest(SapRequestType.INVOICE, "ND0", exemptAmount, null);
                                        sapEvent.fakeSapRequest(SapRequestType.CREDIT, "NA0", exemptAmount, e.getId());
                                    });
                        } else {
                            if (event.getSapRequestSet().stream().filter(sr -> !sr.getIgnore() && sr.getRequest().length() > 2).count() > 0l) {
                                taskLog("Skipping event with sap requests %s%n", event.getExternalId());
                            }
                        }
                    }
                }
            });
        } catch (final Throwable t) {
            taskLog("Unable to process event %s%n", event.getExternalId());
        }
    }

}