package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.studentCurriculum.Substitution;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.YearMonthDay;
import pt.ist.fenixframework.FenixFramework;

public class FixSeparatedRegistration extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Registration registration = FenixFramework.getDomainObject("1972583989820598"); //90527 mestrado
        final StudentCurricularPlan scp = registration.getLastStudentCurricularPlan();
        final YearMonthDay startDate = new YearMonthDay(2023, 02, 13);
        scp.setStartDate(startDate);
        registration.setStartDate(startDate);
        registration.setRegistrationYear(ExecutionYear.readCurrentExecutionYear());
        scp.getCreditsSet().stream()
                .map(Substitution.class::cast)
                .forEach(sub -> sub.setExecutionPeriod(ExecutionSemester.readActualExecutionSemester()));
        RegistrationDataByExecutionYear.getOrCreateRegistrationDataByYear(registration, ExecutionYear.readCurrentExecutionYear());
    }
}
