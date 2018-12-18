package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.AccountingTransaction_Base;
import org.fenixedu.academic.domain.accounting.ResidenceEvent;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.Atomic;

public class CheckMissingNPsForSibsTransaction extends CustomTask {

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(atd -> atd instanceof SibsTransactionDetail)
                .map(atd -> (SibsTransactionDetail) atd)
                .filter(atd -> !(atd.getEvent() instanceof ResidenceEvent))
                .filter(atd -> atd.getSibsLine() != null)
                .filter(atd -> atd.getSibsLine().getHeader().getWhenProcessedBySibs().toLocalDate().getYear() == 2018)
                .filter(atd -> atd.getTransaction().getSapRequestSet().isEmpty())
                .filter(atd -> !atd.getTransaction().getAdjustmentTransactionsSet().isEmpty())
                .forEach(atd -> {
                    boolean hasAnotherTxWithSameSibsLine = atd.getEvent().getAdjustedTransactions().stream()
                            .map(AccountingTransaction_Base::getTransactionDetail)
                            .filter(oatd -> oatd != atd)
                            .filter(oatd -> oatd instanceof SibsTransactionDetail)
                            .filter(oatd -> !(oatd.getEvent() instanceof ResidenceEvent))
                            .map(oatd -> (SibsTransactionDetail) oatd)
                            .filter(oatd -> oatd.getSibsLine() != null)
                            .filter(oatd -> oatd.getTransaction().getAdjustmentTransactionsSet().isEmpty())
                            .anyMatch(oatd -> oatd.getSibsLine().equals(atd.getSibsLine()));
                    if (!hasAnotherTxWithSameSibsLine) {
                        AccountingTransaction transaction = atd.getTransaction().getAdjustmentTransactionsSet().iterator().next();
                        User responsibleUser = transaction.getResponsibleUser();
                        taskLog("%s\t%s\t%s\t%s\t%s\t%s\t%s%n", atd.getExternalId(), atd.getEvent().getExternalId(),
                                atd.getSibsLine().getHeader().getWhenProcessedBySibs().toLocalDate(),
                                atd.getTransaction().getOriginalAmount(), !atd.getTransaction().getAdjustmentTransactionsSet().isEmpty(),
                                transaction.getTransactionDetail().getComments(),
                                responsibleUser != null ? responsibleUser.getDisplayName() : "");
                    }
                });
    }
}
