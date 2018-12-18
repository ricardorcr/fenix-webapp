package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.events.gratuity.StandaloneEnrolmentGratuityEvent;
import org.fenixedu.academic.domain.degree.DegreeType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.util.Collection;

public class ReportStandaloneEvents extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final ExecutionYear sapYear = ExecutionYear.readExecutionYearByName("2012/2013");
        Bennu.getInstance().getAccountingEventsSet().stream()
                .filter(StandaloneEnrolmentGratuityEvent.class::isInstance)
                .filter(e -> e.isOpen())
                .filter(this::isThirdCycle)
                .map(e -> (StandaloneEnrolmentGratuityEvent) e)
                .filter(e -> e.getExecutionYear().isAfter(sapYear))
                .forEach(e -> taskLog("User: %s\tEvento: %s - %s%n", e.getPerson().getUsername(), e.getExternalId(), e.getDescription()));
    }

    private boolean isThirdCycle(final Event event) {
        Collection<Registration> registrationsByDegreeTypes =
                event.getPerson().getStudent().getRegistrationsByDegreeTypes(DegreeType.matching(DegreeType::isThirdCycle).get());
        return !registrationsByDegreeTypes.isEmpty();
    }
}
