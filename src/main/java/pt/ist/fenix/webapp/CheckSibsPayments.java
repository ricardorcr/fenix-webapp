package pt.ist.fenix.webapp;

import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.LocalDate;
import pt.ist.payments.domain.SibsPayment;

public class CheckSibsPayments extends CustomTask {

    @Override
    public void runTask() throws Exception {
        LocalDate date = new LocalDate(2021,11,06);
        final Money value =
                Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(tx -> tx.getTransaction().getSibsPayment() != null)
                .filter(tx -> tx.getTransaction().getSibsPayment().getSettlementDate() != null)
                .filter(tx -> tx.getTransaction().getSibsPayment().getSettlementDate().isEqual(date))
//                .filter(tx -> tx.getTransaction().getSapRequestSet().isEmpty())
//                .forEach(tx -> taskLog("Pagamento sem request associado: %s\t%s%n", tx.getEvent().getExternalId(), tx.getExternalId()));
                .map(tx -> tx.getTransaction().getAmountWithAdjustment())
                .reduce(Money.ZERO, Money::add);
        taskLog("Valor para o dia %s\t%s%n", date.toString(), value.toString());
    }
}
