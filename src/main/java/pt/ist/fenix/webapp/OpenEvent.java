package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventState;
import org.fenixedu.academic.domain.accounting.EventState.ChangeStateEvent;
import org.fenixedu.academic.domain.accounting.calculator.DebtExemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.EventProcessor;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OpenEvent extends CustomTask {

    private static final String BUNDLE = "resources.GiafInvoicesResources";
    private static boolean allowCloseToOpen = false;

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        Arrays.asList("1411197404447385", "1129722427736642", "1411197404447124", "1411197404447050",
                "1129722427736430", "1129722427736603", "1411197404447381", "1411197404447057", "1129722427736698",
                "1129722427736578", "1411197404447507", "1411197404447393", "1129722427736370")
                .forEach(id -> openEvent(id));
    }

    private void openEvent(final String eventId) {
        Event event = FenixFramework.getDomainObject(eventId);
        Money amountToPay = event.calculateAmountToPay(new DateTime());
        if (event.isClosed() && amountToPay.isPositive()) {
            FenixFramework.atomic(() -> {
                Signal.clear(EventState.EVENT_STATE_CHANGED);
                event.open();
                taskLog("Opened event: %s%n", event.getExternalId());
                Signal.register(EventState.EVENT_STATE_CHANGED, this::handlerEventStateChange);
                Signal.register(EventState.EVENT_STATE_CHANGED, this::calculateSapRequestsForCanceledEvent);
                Signal.registerWithoutTransaction(EventState.EVENT_STATE_CHANGED, this::processEvent);
            });
        }
    }

    private void handlerEventStateChange(final ChangeStateEvent eventStateChange) {
        final Event event = eventStateChange.getEvent();
        final EventState oldState = eventStateChange.getOldState();
        final EventState newState = eventStateChange.getNewState();

        /*
         *  NewValue > |  null  | OPEN | CLOSED | CANCELED
         *  OldValue   |________|______|________|__________
         *     V       |
         *    null     |   OK   |  OK  |   Ex   |   Ex
         *    OPEN     |   Ex   |  OK  |   OK   |   SAP
         *   CLOSED    |   Ex   |  Ok  |   OK   |   OK
         *  CANCELED   |   Ex   |  Ex  |   Ex   |   OK
         */

        if (oldState == newState) {
            // Not really a state change... nothing to be done.
        } else if (oldState == null && newState == EventState.OPEN) {
            // Ack, normal SAP integration will be fine.
        } else if (oldState == EventState.OPEN && newState == EventState.CLOSED) {
            // Ack, normal SAP integration will be fine.
        } else if (oldState == EventState.CLOSED && newState == EventState.OPEN) {
            // Ack, normal SAP integration will be fine.
        } else if (oldState == EventState.CLOSED && newState == EventState.CANCELLED) {
            // Ack, normal SAP integration will be fine.
        } else if (oldState == EventState.OPEN && newState == EventState.CANCELLED) {
            if (!new SapEvent(event).canCancel()) {
                throw new DomainException(Optional.of(BUNDLE), "error.event.state.change.first.in.sap");
            }
        } else if (allowCloseToOpen && oldState == EventState.CLOSED && newState == EventState.OPEN) {
            // Ack.
        } else {
            throw new DomainException(Optional.of(BUNDLE), "error.new.event.state.change.must.be.handled", (oldState == null ?
                    "null" : oldState.name()), (newState == null ? "null" : newState.name()), event.getExternalId());
        }
    }

    private void calculateSapRequestsForCanceledEvent(final ChangeStateEvent eventStateChange) {
        final Event event = eventStateChange.getEvent();
        final EventState oldState = eventStateChange.getOldState();
        final EventState newState = eventStateChange.getNewState();

        if (newState == EventState.CANCELLED && oldState != newState && event.getSapRequestSet().isEmpty()) {
            final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
            final Money debtAmount = new Money(calculator.getDebtAmount());
            if (debtAmount.isPositive()) {
                final DebtExemption debtExemption = calculator.getAccountingEntries().stream()
                        .filter(e -> e instanceof DebtExemption)
                        .map(e -> (DebtExemption) e)
                        .filter(e -> new Money(e.getAmount()).equals(debtAmount))
                        .findAny().orElse(null);
                if (debtExemption == null) {
                    throw new Error("inconsistent data, event is canceled but the the exempt value does not match the orginal debt value");
                }
                final SapEvent sapEvent = new SapEvent(event);
                sapEvent.fakeSapRequest(SapRequestType.INVOICE, "ND0", debtAmount, null);
                sapEvent.fakeSapRequest(SapRequestType.CREDIT, "NA0", debtAmount, debtExemption.getId());
            }
        }
    }

    private void processEvent(final ChangeStateEvent eventStateChange) {
        Thread thread = new OpenEvent.ProcessEvent(eventStateChange);
        thread.start();
    }

    private static class ProcessEvent extends Thread {

        private ChangeStateEvent eventStateChange;

        public ProcessEvent(final ChangeStateEvent eventStateChange) {
            setName(this.getClass().getSimpleName());
            this.eventStateChange = eventStateChange;
        }

        @Override
        public void run() {
            EventProcessor.calculate(() -> eventStateChange.getEvent());
            EventProcessor.sync(() -> eventStateChange.getEvent());
        }
    }
}