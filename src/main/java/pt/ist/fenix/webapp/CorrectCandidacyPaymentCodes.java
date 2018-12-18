package pt.ist.fenix.webapp;

import java.math.BigDecimal;

import org.fenixedu.academic.domain.accounting.PaymentCodeState;
import org.fenixedu.academic.domain.accounting.PaymentCodeType;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.YearMonthDay;

public class CorrectCandidacyPaymentCodes extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Money minAmount = new Money(new BigDecimal(100));
        Money maxAmount = new Money(new BigDecimal(5000));
        YearMonthDay endDate = new YearMonthDay(2017, 10, 30);
        Bennu.getInstance()
                .getPaymentCodesSet().stream().filter(pc -> pc.getState().equals(PaymentCodeState.NEW)
                        && pc.getMinAmount().equals(minAmount) && pc.getMaxAmount().equals(maxAmount)
                        && pc.getEndDate().isEqual(endDate))
                .forEach(pc -> {
                    pc.setType(PaymentCodeType.SECOND_CYCLE_INDIVIDUAL_CANDIDACY_PROCESS);
                    pc.setPerson(null);
                });
    }
}
