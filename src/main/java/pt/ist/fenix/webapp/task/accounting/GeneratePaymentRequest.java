package pt.ist.fenix.webapp.task.accounting;

import com.google.common.base.Strings;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.AccountingEntry;
import org.fenixedu.academic.domain.accounting.calculator.CreditEntry;
import org.fenixedu.academic.domain.accounting.calculator.DebtExemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.ExcessRefund;
import org.fenixedu.academic.domain.accounting.calculator.Payment;
import org.joda.time.DateTime;
import pt.ist.fenixedu.giaf.invoices.ErrorLogConsumer;
import pt.ist.fenixedu.giaf.invoices.EventLogger;
import pt.ist.fenixedu.giaf.invoices.EventWrapper;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixedu.giaf.invoices.Utils;
import pt.ist.fenixedu.giaf.invoices.task.SapCustomTask;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.payments.domain.SibsPayment;

import java.math.BigDecimal;

public class GeneratePaymentRequest extends SapCustomTask {

    @Override
    protected void runTask(ErrorLogConsumer errorLog, EventLogger elogger) {
        final Event event = FenixFramework.getDomainObject("290082091173407");
        final SapEvent sapEvent = new SapEvent(event);
        final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
        for (final AccountingEntry accountingEntry : calculator.getAccountingEntries()) {
            if (accountingEntry instanceof Payment && accountingEntry.getAmount().compareTo(BigDecimal.ZERO) > 0
                    && !sapEvent.hasPayment(accountingEntry.getId())
                    && accountingEntry.getCreated().isAfter(EventWrapper.SAP_TRANSACTIONS_THRESHOLD)) {
                final Payment payment = (Payment) accountingEntry;
                if ((payment.getUsedAmountInInterests().signum() > 0 || payment.getUsedAmountInFines().signum() > 0)
                        && !Utils.validateClientData(errorLog, event)) {
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
            } else if (accountingEntry instanceof ExcessRefund && !sapEvent.hasRefund(accountingEntry.getId())) {
                //Reimbursements
                final ExcessRefund excessRefund = (ExcessRefund) accountingEntry;
                if (Strings.isNullOrEmpty(excessRefund.getTargetPaymentId())) {
                    sapEvent.registerReimbursementAdvancement(excessRefund);
                }
            }
        }
    }
}
