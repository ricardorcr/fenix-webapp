package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustificationType;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;

public class RefundOldEvent extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Event.canBeRefunded = (event) -> {return true;};

        final Event event1 = FenixFramework.getDomainObject("1695099037680119");
        event1.refund(User.findByUsername("ist24616"), EventExemptionJustificationType.DIRECTIVE_COUNCIL_AUTHORIZATION, "despacho CG 21/04/2022 #1203842",
                new BigDecimal(1375), "PT50003300004550524974305");

//        //TODO run a 2nd time with this uncommented, after generating the new requests
        final SapRequest refundRequest = new SapRequest(event1, "PT236090321", Money.valueOf(1375), "NR0", SapRequestType.REIMBURSEMENT,
                Money.ZERO, new JsonObject());
        refundRequest.setIntegrated(true);
        refundRequest.setSent(true);

        final SapRequest creditRequest = new SapRequest(event1, "PT236090321", Money.valueOf(1375), "NA0", SapRequestType.CREDIT,
                Money.ZERO, new JsonObject());
        creditRequest.setIntegrated(true);
        creditRequest.setSent(true);

        Event.canBeRefunded = (event) -> {
            final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
            return calculator.getPayments().count() > 0 && calculator.getPayments()
                    .allMatch(p -> p.getDate().getYear() >= 2018);
        };
    }
}