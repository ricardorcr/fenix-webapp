package pt.ist.fenix.webapp;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.LocalDate;
import pt.ist.payments.domain.SibsPayment;

public class CheckSibsPayments extends CustomTask {

    @Override
    public void runTask() throws Exception {
        LocalDate date = new LocalDate(2021,10,27);
        Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(tx -> tx.getTransaction().getSibsPayment() != null)
                .filter(tx -> tx.getWhenRegistered().toLocalDate().isEqual(date))
                .forEach(tx -> taskLog("%s %s%n", tx.getExternalId(), tx.getTransaction().getAmountWithAdjustment()));
    }
}
