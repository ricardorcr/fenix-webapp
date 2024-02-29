package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.degreeStructure.StandaloneCourseGroup;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.StandaloneCurriculumGroup;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class FixSCPStandaloneGroup extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Registration registration = FenixFramework.getDomainObject("1972583989800198");//102343
        final StandaloneCurriculumGroup standaloneCurriculumGroup = registration.getLastStudentCurricularPlan().getStandaloneCurriculumGroup();
        final StandaloneCourseGroup standaloneCourseGroup = registration.getLastStudentCurricularPlan().getDegreeCurricularPlan().getStandaloneCourseGroup();
        standaloneCurriculumGroup.setDegreeModule(standaloneCourseGroup);
    }
}
