package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class CheckSibsPayments extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(tx -> tx.getWhenRegistered().getYear() == 2021)
                .filter(tx -> tx.getWhenRegistered().getMonthOfYear() == 10)
                .filter(tx -> tx.getPaymentMethod().isSibs())
                .filter(tx -> tx.getTransaction().isAdjustingTransaction())
                .forEach(tx -> taskLog("%s %s%n", tx.getExternalId(), tx.getWhenRegistered().toString("dd-MM-yyyy HH:mm")));
    }
}
