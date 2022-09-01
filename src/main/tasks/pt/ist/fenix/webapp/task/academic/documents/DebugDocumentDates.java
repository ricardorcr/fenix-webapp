package pt.ist.fenix.webapp.task.academic.documents;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

public class DebugDocumentDates extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        final DateTime now = new DateTime();
        final ExecutionYear beanYear = ExecutionYear.readByDateTime(now);
        final Registration registration = FenixFramework.getDomainObject("283734129518489");
        final ExecutionYear startYear = registration.getStartExecutionYear();

        Bennu.getInstance().getExecutionYearsSet().stream()
                .filter(y -> y.getName().startsWith("202"))
                .forEach(y -> {
                    taskLog("%s %s: %s - %s%n", y.getExternalId(), y.getName(),
                            y.getBeginLocalDate().toString("yyyy-MM-dd"),
                            y.getEndLocalDate().toString("yyyy-MM-dd"));
                    y.getExecutionPeriodsSet().forEach(p -> {
                        taskLog("   %s %s: %s - %s%n", p.getExternalId(), p.getQualifiedName(),
                                p.getBeginLocalDate().toString("yyyy-MM-dd"),
                                p.getEndLocalDate().toString("yyyy-MM-dd"));
                    });
                });

        taskLog("Bean Year: %s%n", beanYear == null ? "null" : beanYear.getName());
        taskLog("Start Year: %s%n", startYear.getName());
        taskLog("Is Before? %s%n", beanYear == null ? "null" : beanYear.isBefore(startYear));

        final ExecutionYear executionYearToFix = FenixFramework.getDomainObject("565393789812740");
        final ExecutionSemester last = FenixFramework.getDomainObject("566806834053128");
        executionYearToFix.setEndDateYearMonthDay(last.getEndDateYearMonthDay());
    }

}