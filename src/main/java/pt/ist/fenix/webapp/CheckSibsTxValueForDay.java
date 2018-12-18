package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.ResidenceEvent;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.Atomic;

public class CheckSibsTxValueForDay extends CustomTask {

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        final LocalDate dateToCheck = new LocalDate(2018, 07, 26);
//        Money amountTxForSibsDate =
        Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(atd -> atd instanceof SibsTransactionDetail)
                .map(atd -> (SibsTransactionDetail) atd)
                .filter(atd -> !(atd.getEvent() instanceof ResidenceEvent))
                .filter(atd -> atd.getSibsLine() != null)
                .filter(atd -> atd.getSibsLine().getHeader().getWhenProcessedBySibs().toLocalDate().isEqual(dateToCheck))
                .filter(atd -> !atd.getTransaction().getAmountWithAdjustment().equals(atd.getTransaction().getOriginalAmount()))
                .forEach(atd -> taskLog("Esta transacção %s foi ajustada de %s para %s%n",
                        atd.getTransaction().getExternalId(), atd.getTransaction().getOriginalAmount(), atd.getTransaction().getAmountWithAdjustment()));

        Money amountTxForSibsDate = Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(atd -> atd instanceof SibsTransactionDetail)
                .map(atd -> (SibsTransactionDetail) atd)
                .filter(atd -> !(atd.getEvent() instanceof ResidenceEvent))
                .filter(atd -> atd.getSibsLine() != null)
                .filter(atd -> atd.getSibsLine().getHeader().getWhenProcessedBySibs().toLocalDate().isEqual(dateToCheck))
                .map(atd -> atd.getTransaction().getAmountWithAdjustment())
//                .map(atd -> atd.getTransaction().getOriginalAmount())
                .reduce(Money.ZERO, Money::add);
        taskLog("Tenho registado: %s%n", amountTxForSibsDate);
    }
}
