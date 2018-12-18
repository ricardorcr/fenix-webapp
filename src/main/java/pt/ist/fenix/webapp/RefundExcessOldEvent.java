package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

public class RefundExcessOldEvent extends CustomTask {

    @Override
    public void runTask() throws Exception {
        try {
            Event eventToRefund = FenixFramework.getDomainObject("281500746544560");
            Event.canBeRefunded = (event) -> true;
            eventToRefund.refundExcess(User.findByUsername("ist24616"));
        } finally {
            Event.canBeRefunded = (event) -> {
                final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
                return calculator.getPayments().count() > 0 && calculator.getPayments()
                        .allMatch(p -> p.getDate().getYear() >= 2018);
            };
        }
    }
}
