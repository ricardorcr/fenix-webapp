package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.List;

public class DeleteCurricularCourseInDraft extends CustomTask {

    @Override
    public void runTask() throws Exception {

        List<String> courses = Arrays.asList("1971853845333040", "1971853845333047"); //MBR MOTMP - DEAENO
        courses.forEach(id -> {
            final CurricularCourse course = FenixFramework.getDomainObject(id);
            course.getAssociatedExecutionCoursesSet().forEach(ExecutionCourse::delete);
            course.delete();
        });
    }

}
