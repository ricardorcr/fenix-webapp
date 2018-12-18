package pt.ist.fenix.webapp;

import com.google.common.base.Strings;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.AccountingEntry;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.domain.accounting.calculator.DebtExemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.Payment;
import org.fenixedu.academic.util.Money;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixedu.giaf.invoices.DebtCycleType;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.EventWrapper;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;

public class CheckSapEvent extends SapCustomTask {

    public final int yearToComunicate = 2018;

    @Override
    protected void runTask(final ErrorLogConsumer errorLogConsumer, final EventLogger elogger) {
        Event event = FenixFramework.getDomainObject("1407533797350383");
        taskLog("Should process: %s", shouldProcess(null, event));
        processSapTx(errorLogConsumer, elogger, event);
    }

    public boolean shouldProcess(final ErrorLogConsumer consumer, final Event event) {
        return event.getWhenOccured().getYear() <= yearToComunicate
                && event.getSapRequestSet().stream().allMatch(r -> r.getIntegrated())
                && (EventWrapper.needsProcessingSap(event) || needsToProcessPayments(event))
                && Utils.validate(consumer, event);
    }

    public boolean needsToProcessPayments(Event event) {
        return event.getAccountingTransactionsSet().stream().anyMatch(tx -> tx.getWhenRegistered().getYear() == yearToComunicate);
    }

    private void processSapTx(final ErrorLogConsumer consumer, final EventLogger logger, final Event event) {
        try {
            FenixFramework.getTransactionManager().withTransaction(new CallableWithoutException<Void>() {

                @Override
                public Void call() {
                    processSap(consumer, logger, event);
                    return null;
                }
            }, new AtomicInstance(Atomic.TxMode.SPECULATIVE_READ, false));
        } catch (Throwable e) {
            e.printStackTrace();
            try {
                logError(consumer, logger, event, e);
            } catch(Exception ex) {
                taskLog("Não devia dar este erro: %s - %s - %s\n", event.getExternalId(), ex.getMessage(), e.getMessage());
            }

        }
    }

    private void processSap(final ErrorLogConsumer errorLog, final EventLogger elogger, final Event event) {
        if (!shouldProcess(errorLog, event)) {
            return;
        }
        if (EventWrapper.needsProcessingSap(event)) {
            final SapEvent sapEvent = new SapEvent(event);
            if (sapEvent.hasPendingDocumentCancelations()) {
                return;
            }

            final EventWrapper eventWrapper = new EventWrapper(event, errorLog, true);

            sapEvent.updateInvoiceWithNewClientData();

            final Money debtFenix = eventWrapper.debt;
            final Money invoiceSap = sapEvent.getInvoiceAmount();

            if (debtFenix.isPositive()) {
                if (invoiceSap.isZero()) {
                    sapEvent.registerInvoice(debtFenix, event, eventWrapper.isGratuity(), false);
                } else if (invoiceSap.isNegative()) {
                    logError(event, errorLog, elogger, "A dívida no SAP é negativa");
                    return;
                } else if (!debtFenix.equals(invoiceSap)) {
//                    logError(event, errorLog, elogger, "A dívida no SAP é: " + invoiceSap.getAmountAsString()
//                            + " e no Fénix é: " + debtFenix.getAmountAsString());
                    if (debtFenix.greaterThan(invoiceSap)) {
                        // criar invoice com a diferença entre debtFenix e invoiceDebtSap (se for propina aumentar a dívida no sap)
                        // passar data actual (o valor do evento mudou, não dá para saber quando, vamos assumir que mudou quando foi detectada essa diferença)

                        taskLog("Wrong2 %s - %s\n", event.getExternalId(), event.getPerson().getUsername());
//                            sapEvent.registerInvoice(debtFenix.subtract(invoiceSap), eventWrapper.event,
//                                    eventWrapper.isGratuity(), true);
                    } else {
                        // diminuir divida no sap e registar credit note da diferença na última factura existente
                        taskLog("Wrong3 %s - %s\n", event.getExternalId(), event.getPerson().getUsername());
//                            sapEvent.registerCredit(eventWrapper.event, creditEntry, eventWrapper.isGratuity(), errorLog,
//                                    elogger);
//                            ignoreCreditAndInvoice(event, existing);
                    }
                    return;
                }
            }

            DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());

            for (AccountingEntry accountingEntry : calculator.getAccountingEntries()) {
                if (accountingEntry instanceof Payment && accountingEntry.getAmount().compareTo(BigDecimal.ZERO) > 0
                        && !sapEvent.hasPayment(accountingEntry.getId())
                        && ((Payment)accountingEntry).getDate().getYear() == yearToComunicate) {
                    if (Strings.isNullOrEmpty(((Payment) accountingEntry).getRefundId())) {
                        sapEvent.registerPayment((CreditEntry) accountingEntry);
                    } else {
                        //TODO process payment made from refund
                        return;
                    }
                } else if (accountingEntry instanceof DebtExemption) {
                    if (accountingEntry.getAmount().compareTo(BigDecimal.ZERO) > 0 && !sapEvent.hasCredit(accountingEntry.getId())
                            && accountingEntry.getCreated().getYear() == yearToComunicate) {
//                        sapEvent.registerCredit(event, (CreditEntry) accountingEntry, eventWrapper.isGratuity());
                    }
                }
            }

        } else {
            //processing payments of past events
//                eventWrapper.paymentsSap().filter(d -> !sapEvent.hasPayment(d)).peek(
//                    d -> elogger.log("Processing past payment %s : %s%n", eventWrapper.event.getExternalId(), d.getExternalId()))
//                    .forEach(d -> sapEvent.registerInvoiceAndPayment(clientMap, d, errorLog, elogger));
        }
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

    private static void logError(Event event, ErrorLogConsumer errorLog, EventLogger elogger, String errorMessage) {
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
        elogger.log("%s: %s %s %s %n", event.getExternalId(), errorMessage, "", "");
    }

    static CreditEntry getCreditEntry(final Money creditAmount) {
        return new CreditEntry("", new DateTime(), new LocalDate(), "", creditAmount.getAmount()) {
            @Override
            public boolean isToApplyInterest() {
                return false;
            }

            @Override
            public boolean isToApplyFine() {
                return false;
            }

            @Override
            public boolean isForInterest() {
                return false;
            }

            @Override
            public boolean isForFine() {
                return false;
            }

            @Override
            public boolean isForDebt() {
                return false;
            }
        };
    }
}
