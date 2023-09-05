package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.LocalDate;

public class TestUCIsCreateEventsFromTemplate extends CustomTask {

    @Override
    public void runTask() throws Exception {
        ExecutionYear.readCurrentExecutionYear().getRegistrationDataByExecutionYearSet().stream()
                .filter(rdey -> rdey.getRegistration().getNumber() == 102697)
                .forEach(this::process);

    }

    private void process(final RegistrationDataByExecutionYear dataByExecutionYear) {
        dataByExecutionYear.edit(new LocalDate().minusDays(15), dataByExecutionYear.getEventTemplate());
        final EventTemplate eventTemplate = EventTemplate.templateFor(dataByExecutionYear);
        if (eventTemplate != null) {
            eventTemplate.createEventsFor(dataByExecutionYear);
        }
    }
}
