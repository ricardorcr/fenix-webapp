package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.Refund;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustificationType;
import org.fenixedu.academic.domain.accounting.events.gratuity.exemption.penalty.FixedAmountInterestExemption;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.FenixFramework;

public class CreatePenaltyExemptionInPast extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Event event = FenixFramework.getDomainObject("3104831858300270");
        final User responsible = User.findByUsername("ist24616");
        final LocalDate beforePayment = new LocalDate(2023,11,12);
        FixedAmountInterestExemption exemption = new FixedAmountInterestExemption(event, responsible.getPerson(),
                new Money(0.01), EventExemptionJustificationType.FINE_EXEMPTION, beforePayment.toDateTimeAtCurrentTime(),
                "Na altura em que foi feito o pagamento não existiam juros em dívida. É para continuar assim.");
        exemption.setWhenCreated(beforePayment.toDateTimeAtCurrentTime());
    }
}
