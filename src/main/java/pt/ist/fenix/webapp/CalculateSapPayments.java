package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;
import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.giaf.invoices.*;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;
import java.time.Year;

public class CalculateSapPayments extends SapCustomTask {

    private final int currentYear = Year.now().getValue();

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    protected void runTask(ErrorLogConsumer consumer, EventLogger logger) {
        if (EventWrapper.SAP_THRESHOLD == null) {
            throw new Error();
        }
        Bennu.getInstance().getAccountingEventsSet().stream().parallel().forEach(e -> processSapTx(consumer, logger, e));
    }

    public boolean shouldProcessPayments(final ErrorLogConsumer consumer, final Event event, final DebtInterestCalculator calculator) {
        return event.getWhenOccured().getYear() >= currentYear
                && event.getSapRequestSet().stream().allMatch(SapRequest::getIntegrated)
                && EventWrapper.needsProcessingSap(event)
                && needsToProcessPayments(calculator)
                && Utils.validate(consumer, event);
    }

    private boolean needsToProcessPayments(final DebtInterestCalculator calculator) {
        return calculator.getPayments().anyMatch(p -> p.getDate().getYear() == currentYear);
    }

    private void processSapTx(final ErrorLogConsumer consumer, final EventLogger logger, final Event event) {
        try {
            FenixFramework.getTransactionManager().withTransaction((CallableWithoutException<Void>) () -> {
                processSap(consumer, event);
                return null;
            }, new AtomicInstance(TxMode.SPECULATIVE_READ, false));
        } catch (Throwable e) {
            logError(consumer, logger, event, e);
            e.printStackTrace();
        }
    }

    private void processSap(final ErrorLogConsumer errorLog, final Event event) {
        DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
        if (!shouldProcessPayments(errorLog, event, calculator)) {
            return;
        }
        if (EventWrapper.needsProcessingSap(event)) {

            if(hasOnlyPayments(event, calculator)) {

                final SapEvent sapEvent = new SapEvent(event);
                if (sapEvent.hasPendingDocumentCancelations()) {
                    return;
                }

                //Payments
                calculator.getPayments().filter(p -> p.getAmount().compareTo(BigDecimal.ZERO) > 0
                        && p.getCreated().isAfter(EventWrapper.SAP_TRANSACTIONS_THRESHOLD)
                        && !sapEvent.hasPayment(p.getId()))
                        .forEach(sapEvent::registerPayment);
            }
        } else {
            //processing payments of past events
//                eventWrapper.paymentsSap().filter(d -> !sapEvent.hasPayment(d)).peek(
//                    d -> elogger.log("Processing past payment %s : %s%n", eventWrapper.event.getExternalId(), d.getExternalId()))
//                    .forEach(d -> sapEvent.registerInvoiceAndPayment(clientMap, d, errorLog, elogger));
        }
    }

    private boolean hasOnlyPayments(final Event event, final DebtInterestCalculator calculator) {
        return event.getRefundSet().isEmpty() && event.getDiscountsSet().isEmpty()
                && calculator.getDebtExemptionAmount().signum() != 1
                && (calculator.getPayments().count() == 1 ||
                        calculator.getPayments().noneMatch(p -> p.getUsedAmountInDebts().signum() == 1 && p.getAmountInAdvance().signum() == 1))
                && calculator.getPayments().allMatch(p -> p.getRefundId().isEmpty());
    }

    private static void logError(final ErrorLogConsumer errorLog, final EventLogger elogger, final Event event,
                                 final Throwable e) {
        final String errorMessage = e.getMessage();

        BigDecimal amount;
        DebtCycleType cycleType;

        try {
            amount = event.getOriginalAmountToPay().getAmount();
            cycleType = Utils.cycleType(event);
        } catch (Exception ex) {
            amount = null;
            cycleType = null;
        }

        errorLog.accept(event.getExternalId(), Utils.getUserIdentifier(event.getParty()), event.getParty().getName(),
                amount == null ? "" : amount.toPlainString(), cycleType == null ? "" : cycleType.getDescription(), errorMessage,
                "", "", "", "", "", "", "", "", "", "", "");
        elogger.log("%s: %s%n", event.getExternalId(), errorMessage);
        elogger.log(
                "Unhandled error for event " + event.getExternalId() + " : " + e.getClass().getName() + " - " + errorMessage);
        e.printStackTrace();
    }
}