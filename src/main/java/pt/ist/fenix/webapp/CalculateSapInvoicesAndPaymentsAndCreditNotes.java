package pt.ist.fenix.webapp;

import com.google.common.base.Strings;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.AccountingEntry;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.domain.accounting.calculator.DebtExemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.ExcessRefund;
import org.fenixedu.academic.domain.accounting.calculator.Payment;
import org.fenixedu.academic.domain.accounting.calculator.Refund;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
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
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.CallableWithoutException;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;
import java.util.List;

public class CalculateSapInvoicesAndPaymentsAndCreditNotes extends SapCustomTask {

    //TODO os reimbursements não estão a ser processados!

    //private final String FILENAME = "/afs/ist.utl.pt/ciist/fenix/fenix036/InvoicesToTransfer.csv";
    //private final String FILENAME = "/home/rcro/Documents/fenix/sap/InvoicesToTransfer.csv";
    private final List<String> exceptionEvents = (List<String>) Arrays.asList(new String[] {"1974336336429062", "1411386383007837"/*, "1407400653358156"*/});
    private final int yearToComunicate = 2018;

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
//        processSapTx(consumer, logger, FenixFramework.getDomainObject("1407400653358156"));
    }

    public boolean shouldProcess(final ErrorLogConsumer consumer, final Event event) {
        return event.getWhenOccured().getYear() <= yearToComunicate
                && event.getSapRequestSet().stream().allMatch(r -> r.getIntegrated())
                && (EventWrapper.needsProcessingSap(event) || needsToProcessPayments(event))
                && (Utils.validate(consumer, event) || isException(event));
    }

    private boolean isException(Event event) {
        return exceptionEvents.contains(event.getExternalId());
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
            }, new AtomicInstance(TxMode.SPECULATIVE_READ, false));
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

        final SapEvent sapEvent = new SapEvent(event);
        if (EventWrapper.needsProcessingSap(event)) {
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
                        && accountingEntry.getDate().getYear() == yearToComunicate) {
                    Payment payment = (Payment) accountingEntry;
                    if (Strings.isNullOrEmpty(((Payment) accountingEntry).getRefundId())) {
                        sapEvent.registerPayment(payment);
                    } else {
                        sapEvent.registerAdvancementInPayment(payment);
                    }
                } else if (accountingEntry instanceof DebtExemption) {
                    if (accountingEntry.getAmount().compareTo(BigDecimal.ZERO) > 0 && !sapEvent.hasCredit(accountingEntry.getId())
                            && accountingEntry.getCreated().getYear() == yearToComunicate) {
//                        sapEvent.registerCredit(event, (CreditEntry) accountingEntry, eventWrapper.isGratuity());
                    }
                } else if (accountingEntry instanceof Refund && accountingEntry.getDate().getYear() == yearToComunicate
                        && !sapEvent.hasRefund(accountingEntry.getId())) {
                    //Reimbursements
                    Refund refund = (Refund) accountingEntry;
                    //sapEvent.registerReimbursement(refund);
                } else if (accountingEntry instanceof ExcessRefund && accountingEntry.getDate().getYear() == yearToComunicate
                        && !sapEvent.hasRefund(accountingEntry.getId())) {
                    //Reimbursements
                    ExcessRefund excessRefund = (ExcessRefund) accountingEntry;
                    if (Strings.isNullOrEmpty(excessRefund.getTargetPaymentId())) {
                        sapEvent.registerReimbursementAdvancement(excessRefund);
                    }
                }
            }
        } else {
            //processing payments of past events
            DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
            calculator.getPayments().filter(p -> !sapEvent.hasPayment(p.getId()) && p.getCreated().isAfter(EventWrapper.SAP_TRANSACTIONS_THRESHOLD))
                    .forEach(p -> sapEvent.registerPastPayment(p));
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