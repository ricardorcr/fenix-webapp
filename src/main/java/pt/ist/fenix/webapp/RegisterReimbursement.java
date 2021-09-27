package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.AccountingEntry;
import org.fenixedu.academic.domain.accounting.calculator.DebtExemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.calculator.Refund;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.FenixFramework;

public class RegisterReimbursement extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Event event = FenixFramework.getDomainObject("5016522793583");
        SapEvent sapEvent = new SapEvent(event);
        final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
        for (final AccountingEntry accountingEntry : calculator.getAccountingEntries()) {
            if (accountingEntry instanceof Refund && !sapEvent.hasRefund(accountingEntry.getId())) {
                //Reimbursements
                final Refund refund = (Refund) accountingEntry;
                final DebtExemption debtExemption = findDebtExemptionfor(calculator, refund);
                sapEvent.registerReimbursement(refund, debtExemption);
            }
        }
    }

    private DebtExemption findDebtExemptionfor(final DebtInterestCalculator calculator, final Refund refund) {
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
