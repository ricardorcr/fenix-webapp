package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.events.gratuity.EnrolmentGratuityEvent;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

import java.math.BigDecimal;

public class FixStandaloneGratuity extends CustomTask {

    Person responsible = null;

    @Override
    public void runTask() throws Exception {
        responsible = User.findByUsername("ist24616").getPerson();

        ExecutionYear.readCurrentExecutionYear().getAnnualEventsSet().stream()
                .filter(EnrolmentGratuityEvent.class::isInstance)
                .map(EnrolmentGratuityEvent.class::cast)
                .filter(event -> EventType.STANDALONE_PER_ENROLMENT_GRATUITY == event.getEventType())
                .filter(event -> !event.isCancelled())
                .forEach(this::fix);
    }

    private void fix(EnrolmentGratuityEvent event) {
        final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
        final BigDecimal originalAmountToPay = calculator.getDebtAmount();
        final BigDecimal amountToPay = calculator.getDueAmount();
        final BigDecimal interests = calculator.getDueInterestAmount();
        if (originalAmountToPay.compareTo(amountToPay) == 0 || originalAmountToPay.add(interests).compareTo(amountToPay) == 0) {
            event.cancel(responsible, "Gerado com a fórmula errada");
            taskLog("Evento cancelado: %s\t%s\t%s%n", event.getExternalId(), event.getDescription(), event.getPerson().getUsername());
        } else {
            //report
            taskLog("Já tem cenas lançadas: %s\t%s\t%s%n", event.getExternalId(), event.getDescription(), event.getPerson().getUsername());
        }
    }
}
