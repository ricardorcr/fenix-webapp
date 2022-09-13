package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.ResidenceEvent;
import org.fenixedu.academic.domain.accounting.accountingTransactions.detail.SibsTransactionDetail;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.joda.time.LocalDate;
import pt.ist.fenixedu.domain.SapRequestType;

public class CheckMissingValueForSibsTx extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final LocalDate dateToCheck = new LocalDate(2021, 8, 26);
        Bennu.getInstance().getAccountingTransactionDetailsSet().stream()
                .filter(atd -> atd instanceof SibsTransactionDetail)
                .map(atd -> (SibsTransactionDetail) atd)
                .filter(atd -> !(atd.getEvent() instanceof ResidenceEvent))
                .filter(atd -> atd.getSibsLine() != null)
                .filter(atd -> atd.getSibsLine().getHeader().getWhenProcessedBySibs().toLocalDate().isEqual(dateToCheck))
                .filter(this::isMissingValue)
                .forEach(atd -> taskLog("%s\t%s\t%s%n", atd.getExternalId(), atd.getTransaction().getAmountWithAdjustment(), atd.getEvent().getExternalId()));
    }

    private boolean isMissingValue(SibsTransactionDetail sibsDetail) {
        final Money sibsValue = sibsDetail.getTransaction().getAmountWithAdjustment();
        final Money requestMoney = sibsDetail.getTransaction().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.ADVANCEMENT || sr.getRequestType() == SapRequestType.PAYMENT
                        || sr.getRequestType() == SapRequestType.PAYMENT_INTEREST)
                .map(sr -> sr.getValue().add(sr.getAdvancement()))
                .reduce(Money.ZERO, Money::add);
        return !sibsValue.equals(requestMoney);
    }
}
