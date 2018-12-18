package pt.ist.fenix.webapp;

import com.google.common.base.Strings;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.*;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustificationType;
import org.fenixedu.academic.util.Money;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.EventWrapper;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ForceSapRequestGeneration extends SapCustomTask {

    @Override
    protected void runTask(ErrorLogConsumer errorLogConsumer, EventLogger elogger) {
        List<String> eventIDs = null;
//        try {
//            eventIDs = Files.readAllLines(
//                    new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/events_to_generate.txt").toPath());
            eventIDs = Arrays.asList("1125925676649851","281500746548063","1978579764117841","1978579764117841","1125925676651199","571204880564843","571204880564843","281500746546371","1125925676651848","281500746546988","281500746548243","1131066752501258","1415599745936146","1415599745936146","1415599745936146","1407533797355363","1407533797355363","281500746547498","1125925676650515","281500746548242","1125925676652397","1125925676652510","281500746548340","1126058820636492","1134124769218400","1125925676649915","1125925676650075");
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
                    taskLog("Problems: %s%n", event.getExternalId());
                }
            }

            final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
            for (final AccountingEntry accountingEntry : calculator.getAccountingEntries()) {
                if (accountingEntry instanceof Payment && accountingEntry.getAmount().compareTo(BigDecimal.ZERO) > 0
                        && !sapEvent.hasPayment(accountingEntry.getId())
                        && accountingEntry.getCreated().isAfter(EventWrapper.SAP_TRANSACTIONS_THRESHOLD)) {
                    final Payment payment = (Payment) accountingEntry;

                    if (offsetPayments && payment.getCreated().plusDays(7).isAfterNow()) {
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
                    .filter(p -> !offsetPayments || p.getCreated().plusDays(15).isBeforeNow())
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
}
