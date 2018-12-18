package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.PaymentMethod;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.YearMonthDay;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixframework.Atomic;

public class CheckSIBSTransactionsSapRequests extends CustomTask {

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        final YearMonthDay day = new YearMonthDay(2019,01,02);
        final Money[] txsTotal = new Money[] { Money.ZERO };
        Bennu.getInstance().getAccountingTransactionsSet().stream()
                .filter(tx -> tx.getPaymentMethod() == PaymentMethod.getSibsPaymentMethod())
                .filter(tx -> tx.getTransactionDetail() instanceof SibsTransactionDetail)
                .filter(tx -> tx.getAdjustedTransaction() == null)
                .map(tx -> (SibsTransactionDetail)tx.getTransactionDetail())
                .filter(atx -> atx.getSibsLine() != null && atx.getSibsLine().getHeader().getWhenProcessedBySibs().isEqual(day))
                .forEach(atx -> {
                    Money total = atx.getTransaction().getSapRequestSet().stream()
                            .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.ADVANCEMENT || sr.getRequestType() == SapRequestType.PAYMENT_INTEREST)
                            .map(sr -> sr.getValue().add(sr.getAdvancement()))
                            .reduce(Money.ZERO, Money::add);
                    if (!atx.getTransaction().getOriginalAmount().equals(total)) {
                        taskLog("Ver evento: %s tx: %s %s - requests: %s%n", atx.getEvent().getExternalId(), atx.getTransaction().getExternalId(), atx.getTransaction().getOriginalAmount(), total);
                    }
                    if (atx.getWhenRegistered().getYear() < 2019) {
                        taskLog("És tuuuuu: %s%n", atx.getExternalId());
                    }
                    txsTotal[0] = txsTotal[0].add(atx.getSibsLine().getAmount()/*atx.getTransaction().getOriginalAmount()*/);
                });
        taskLog("Valor total das transacções SIBS: %s%n", txsTotal[0]);
    }
}
