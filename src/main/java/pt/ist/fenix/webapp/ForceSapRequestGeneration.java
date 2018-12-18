package pt.ist.fenix.webapp;

import com.google.common.base.Strings;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.*;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustificationType;
import org.fenixedu.academic.util.Money;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.DebtCycleType;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.EventWrapper;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.payments.domain.SibsPayment;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ForceSapRequestGeneration extends SapCustomTask {

    private static int PAYMENT_OFFSET = 7; //days

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        List<String> eventIDs = null;
//        try {
//            eventIDs = Files.readAllLines(
//                    new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/events_to_generate.txt").toPath());
            eventIDs = Arrays.asList("1125925676657789");
//        } catch (IOException e) {
//            throw new Error("Erro a ler o ficheiro.");
//        }

        Set<String> ids = new HashSet<>(eventIDs);
        for (String eventId: ids) {
            Event event = FenixFramework.getDomainObject(eventId);
            try {
                FenixFramework.atomic(() -> processSap(errorLogConsumer, elogger, event, false));
            } catch (Exception e) {
                taskLog("Error processing event: %s %s%n", event.getExternalId(), e.getMessage());
                e.printStackTrace();
            }
        }

    }

    private void processSap(final ErrorLogConsumer errorLog, final EventLogger elogger, final Event event, final boolean offsetPayments) {
//        if (!EventWrapper.shouldProcess(errorLog, event)) {
//            return;
//        }

        final SapEvent sapEvent = new SapEvent(event);
        if (sapEvent.hasPendingDocumentCancelations()) {
            return;
        }
        if (EventWrapper.needsProcessingSap(event)) {

            final EventWrapper eventWrapper = new EventWrapper(event, errorLog, true);

            sapEvent.updateInvoiceWithNewClientData();

            final Money debtFenix = eventWrapper.debt;
            final Money invoiceSap = sapEvent.getInvoiceAmount();

            if (debtFenix.isPositive()) {
                if (invoiceSap.isZero()) {
                    sapEvent.registerInvoice(debtFenix, event, eventWrapper.isGratuity(), false);
                } else if (invoiceSap.isNegative()) {
                    logError(event, errorLog, elogger, "A dívida no SAP é negativa");
                }
            }

            final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
            for (final AccountingEntry accountingEntry : calculator.getAccountingEntries()) {
                if (accountingEntry instanceof Payment && accountingEntry.getAmount().compareTo(BigDecimal.ZERO) > 0
                        && !sapEvent.hasPayment(accountingEntry.getId())
                        && accountingEntry.getCreated().isAfter(EventWrapper.SAP_TRANSACTIONS_THRESHOLD)) {
                    final Payment payment = (Payment) accountingEntry;

                    if (offsetPayments && payment.getCreated().plusDays(PAYMENT_OFFSET).isAfterNow()) {
                        return;
                    }

                    final AccountingTransaction accountingTransaction = ((AccountingTransaction) FenixFramework.getDomainObject(payment.getId()));
                    final SibsPayment sibsPayment = accountingTransaction.getSibsPayment();
                    if (sibsPayment != null && sibsPayment.getSettlementDate() == null) {
                        return;
                    }

                    if (Strings.isNullOrEmpty(payment.getRefundId())) {
                        sapEvent.registerPayment((CreditEntry) accountingEntry);
                    } else {
                        sapEvent.registerAdvancementInPayment(payment);
                    }
                } else if (accountingEntry instanceof DebtExemption) {
                    if (accountingEntry.getAmount().compareTo(BigDecimal.ZERO) > 0 && !sapEvent.hasCredit(accountingEntry.getId())
                            && accountingEntry.getCreated().isAfter(EventWrapper.SAP_TRANSACTIONS_THRESHOLD)) {
                        final DebtExemption debtExemption = (DebtExemption) accountingEntry;
                        if (EventExemptionJustificationType.CUSTOM_PAYMENT_PLAN.name().equals(debtExemption.getDescription())) {
                            final Money value = new Money(debtExemption.getAmount());
                            sapEvent.fakeSapRequest(SapRequestType.INVOICE, "ND0", value, null);
                            sapEvent.fakeSapRequest(SapRequestType.CREDIT, "NA0", value, debtExemption.getId());
                        } else {
                            sapEvent.registerCredit(event, debtExemption, eventWrapper.isGratuity(), false);
                        }
                    }
                } else if (accountingEntry instanceof Refund && !sapEvent.hasRefund(accountingEntry.getId())) {
                    //Reimbursements
                    final Refund refund = (Refund) accountingEntry;
                    final DebtExemption debtExemption = findDebtExemptionfor(calculator, refund);
                    sapEvent.registerReimbursement(refund, debtExemption);
                } else if (accountingEntry instanceof ExcessRefund && !sapEvent.hasRefund(accountingEntry.getId())) {
                    //Reimbursements
                    final ExcessRefund excessRefund = (ExcessRefund) accountingEntry;
                    if (Strings.isNullOrEmpty(excessRefund.getTargetPaymentId())) {
                        sapEvent.registerReimbursementAdvancement(excessRefund);
                    }
                }
            }
        } else {
            //processing payments of past events
            DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
            calculator.getPayments().filter(p -> !sapEvent.hasPayment(p.getId()) && !sapEvent.hasCredit(p.getId())
                    && p.getCreated().isAfter(EventWrapper.SAP_TRANSACTIONS_THRESHOLD))
                    .filter(p -> !offsetPayments || p.getCreated().plusDays(PAYMENT_OFFSET).isBeforeNow())
                    .forEach(p -> sapEvent.registerPastPayment(p));
        }
    }

    private static DebtExemption findDebtExemptionfor(final DebtInterestCalculator calculator, final Refund refund) {
        AccountingEntry previousAccountingEntry = null;
        for (final AccountingEntry accountingEntry : calculator.getAccountingEntries()) {
            if (accountingEntry instanceof DebtExemption && previousAccountingEntry == refund) {
                return (DebtExemption) accountingEntry;
            }
            if (accountingEntry instanceof Refund) {
                previousAccountingEntry = accountingEntry;
            }
        }
        return null;
    }

    @Atomic(mode = Atomic.TxMode.READ)
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

        final String documentNumbers = event.getSapRequestSet().stream()
                .filter(sr -> sr.getSent())
                .filter(sr -> !sr.getIntegrated())
                .map(sr -> sr.getDocumentNumber())
                .collect(Collectors.joining(", "));
        final String errorMessageExtraInfo = Stream.concat(Stream.of(errorMessage),
                event.getSapRequestSet().stream()
                        .filter(sr -> sr.getSent())
                        .filter(sr -> !sr.getIntegrated())
                        .flatMap(sr -> sr.getErrorMessages().stream()))
                .filter(m -> m != null)
                .collect(Collectors.joining(", "));
        errorLog.accept(event.getExternalId(), Utils.getUserIdentifier(event.getParty()), event.getParty().getName(),
                amount == null ? "" : amount.toPlainString(), cycleType == null ? "" : cycleType.getDescription(), errorMessageExtraInfo,
                "", "", "", "", "", "", "", "", "", documentNumbers, "");
        elogger.log("%s: %s %s %s %n", event.getExternalId(), errorMessage, "", "");
    }
}
