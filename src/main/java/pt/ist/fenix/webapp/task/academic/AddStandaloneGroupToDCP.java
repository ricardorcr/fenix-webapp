package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.StandaloneCourseGroup;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class AddStandaloneGroupToDCP extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final CourseGroup lmac_2024 = FenixFramework.getDomainObject("565544113668107");
        new StandaloneCourseGroup(lmac_2024, "Unidades Curriculares Isoladas", "Standalone Curricular Courses",
        ExecutionSemester.readActualExecutionSemester(), null, null,
        null, null);
    }
}
