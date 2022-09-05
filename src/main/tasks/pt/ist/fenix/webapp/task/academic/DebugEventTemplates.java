package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.accounting.EventTemplateConfig;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

public class DebugEventTemplates extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final Student student = Student.readStudentByNumber(96542);
        for (final Registration registration : student.getRegistrationsSet()) {
            taskLog("Registration: %s%n", registration.getDegree().getPresentationName());
            final EventTemplate eventTemplate = registration.getEventTemplate();
            taskLog("Registration event template: %s%n", eventTemplate == null ? null : eventTemplate.getTitle().getContent());
            for (final RegistrationDataByExecutionYear dataByYear : registration.getRegistrationDataByExecutionYearSet()) {
                EventTemplate eventTemplateForYear = dataByYear.getEventTemplate();
                if (eventTemplateForYear == null) {
                    eventTemplateForYear = eventTemplate;
                }
/*
                final EventTemplateConfig templateConfig = eventTemplateForYear.getConfigFor(dataByYear.getExecutionYear()
                        .getBeginLocalDate().plusDays(12).toDateTimeAtStartOfDay());
*/
            }
        }
    }
}