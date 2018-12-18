package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.PaymentCode;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

public class CheckMissingSibsLines extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        long count = Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(SibsTransactionDetail.class::isInstance)
                .map(atd -> (SibsTransactionDetail) atd)
                .filter(atd -> atd.getSibsLine() == null)
                .count();
        taskLog("There are %s sibs transactions without sibsLine%n%n", count);

        Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(SibsTransactionDetail.class::isInstance)
                .map(atd -> (SibsTransactionDetail) atd)
                .filter(atd -> atd.getSibsLine() == null)
                .filter(atd -> atd.getWhenRegistered().getYear() >= 2018)
                //.filter(this::doesNotExistOtherTransaction)
                //.filter((this::codeIsNotMapped))
                .forEach(atd -> taskLog("Transaction %s code %s %s without sibsLine%n",
                        atd.getExternalId(), atd.getSibsCode(), atd.getWhenRegistered()));
    }

    private boolean codeIsNotMapped(final SibsTransactionDetail sibsTransactionDetail) {
        PaymentCode paymentCode = PaymentCode.readByCode(sibsTransactionDetail.getSibsCode());
        return paymentCode.getNewPaymentCodeMappingsSet().isEmpty() && paymentCode.getOldPaymentCodeMappingsSet().isEmpty();
    }

    private boolean doesNotExistOtherTransaction(final SibsTransactionDetail sibsTransactionDetail) {
        return !Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(SibsTransactionDetail.class::isInstance)
                .map(atd -> (SibsTransactionDetail) atd)
                .filter(atd -> atd.getSibsLine() != null)
                .filter((atd -> atd.getSibsCode().equals(sibsTransactionDetail.getSibsCode())))
                .findAny().isPresent();
    }
}
