package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class FixStudentCurricularPlanStartDate extends CustomTask {

    public void runTask() throws Exception {
        final Registration registration = FenixFramework.getDomainObject("283734129518808"); //97331
        registration.getLastStudentCurricularPlan().setStartDate(ExecutionYear.readCurrentExecutionYear().getBeginDateYearMonthDay());
    }
}
