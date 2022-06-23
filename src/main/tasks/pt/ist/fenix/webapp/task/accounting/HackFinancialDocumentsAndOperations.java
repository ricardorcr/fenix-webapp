package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Refund;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Set;
import java.util.stream.Collectors;

public class HackFinancialDocumentsAndOperations extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
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
    }

}