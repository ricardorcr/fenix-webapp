package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.accounting.EventTemplateConfig;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

public class DebugEventTemplates extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final Registration registration = Registration.readByNumber(105312).iterator().next();
        final EventTemplate eventTemplate = registration.getEventTemplate();
        for (final RegistrationDataByExecutionYear dataByYear : registration.getRegistrationDataByExecutionYearSet()) {
            EventTemplate eventTemplateForYear = dataByYear.getEventTemplate();
            if (eventTemplateForYear == null) {
                eventTemplateForYear = eventTemplate;
            }
            final EventTemplateConfig templateConfig = eventTemplateForYear.getConfigFor(dataByYear.getExecutionYear()
                    .getBeginLocalDate().plusDays(12).toDateTimeAtStartOfDay());

        }
    }
}