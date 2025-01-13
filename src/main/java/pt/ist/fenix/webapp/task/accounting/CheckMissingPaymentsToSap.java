package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

public class CheckMissingPaymentsToSap extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getAccountingTransactionsSet().stream()
                .filter(tx -> tx.getWhenRegistered().getYear() == 2024)
                .filter(tx -> tx.getSapRequestSet().isEmpty())
                .filter(tx -> !tx.isAdjustingTransaction() && tx.getAdjustmentTransactionsSet().isEmpty())
                .forEach(tx -> taskLog("%s\t%s\t%s\t%s\t%s\t%s%n", tx.getEvent().getExternalId(), tx.getExternalId(),
                        tx.getPaymentMethod().getDescription().getContent(), tx.getOriginalAmount(),
                        tx.getWhenRegistered().toString("dd/MM/yyyy HH:mm:ss"),
                        tx.getWhenProcessed().toString("dd/MM/yyyy HH:mm:ss")));

    }
}
