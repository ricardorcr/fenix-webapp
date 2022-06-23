package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Refund;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.FenixFramework;

public class HackFinancialDocumentsAndOperations extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
/*
        final Refund refund = FenixFramework.getDomainObject("571277895012736");
        final AccountingTransaction toDelete = FenixFramework.getDomainObject("564470371935314");
        final Event event = toDelete.getEvent();
        final Set<AccountingTransaction> others = event.getAccountingTransactionsSet().stream()
                .filter(tx -> tx != toDelete)
                .peek(tx -> tx.setEvent(null))
                .collect(Collectors.toSet());
        refund.delete();
//        toDelete.delete();
        others.forEach(tx -> tx.setEvent(event));
 */

        final Event event = FenixFramework.getDomainObject("571204880564868");
        final Refund refund = FenixFramework.getDomainObject("571277895012808");
        final SapEvent sapEvent = new SapEvent(event);
        final SapRequest sapRequest = sapEvent.fakeSapRequest(SapRequestType.REIMBURSEMENT, "NR0", new Money("0.07"), null);
        sapRequest.setRefund(refund);
    }

}