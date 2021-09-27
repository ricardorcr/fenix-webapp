package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.EnrolmentPeriodInCurricularCourses;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

public class CreateOldDCPsEnrolmentPeriods extends CustomTask {

    @Override
    public void runTask() throws Exception {
        DateTime startDate = new DateTime(2021,9,1,0,0);
        DateTime endDate = new DateTime(2021,9,30,0,0);
        DegreeCurricularPlan leic_aDCP = FenixFramework.getDomainObject("2581275345327");
        new EnrolmentPeriodInCurricularCourses(leic_aDCP, ExecutionSemester.readActualExecutionSemester(), startDate, endDate);
    }
}
