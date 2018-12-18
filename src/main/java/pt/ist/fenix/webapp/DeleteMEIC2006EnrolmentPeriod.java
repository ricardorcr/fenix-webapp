package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.EnrolmentPeriodInClasses;
import org.fenixedu.academic.domain.EnrolmentPeriodInClassesMobility;
import org.fenixedu.academic.domain.EnrolmentPeriodInCurricularCourses;
import org.fenixedu.academic.domain.EnrolmentPeriodInCurricularCoursesSpecialSeason;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixframework.FenixFramework;

public class DeleteMEIC2006EnrolmentPeriod extends CustomTask {

    @Override
    public void runTask() throws Exception {
        DegreeCurricularPlan meicA2006 = FenixFramework.getDomainObject("2581275345328");
        DegreeCurricularPlan meicT2006 = FenixFramework.getDomainObject("2581275345439");

        ExecutionSemester executionSemester = ExecutionSemester.readActualExecutionSemester();

        meicA2006
                .getEnrolmentPeriodsSet()
                .stream()
                .filter(ep -> (ep instanceof EnrolmentPeriodInClasses || ep instanceof EnrolmentPeriodInCurricularCourses
                        || ep instanceof EnrolmentPeriodInCurricularCoursesSpecialSeason || ep instanceof EnrolmentPeriodInClassesMobility)
                        && ep.getExecutionPeriod() == executionSemester).forEach(ep -> ep.delete());

        meicT2006
                .getEnrolmentPeriodsSet()
                .stream()
                .filter(ep -> (ep instanceof EnrolmentPeriodInClasses || ep instanceof EnrolmentPeriodInCurricularCourses
                        || ep instanceof EnrolmentPeriodInCurricularCoursesSpecialSeason || ep instanceof EnrolmentPeriodInClassesMobility)
                        && ep.getExecutionPeriod() == executionSemester).forEach(ep -> ep.delete());
    }
}
