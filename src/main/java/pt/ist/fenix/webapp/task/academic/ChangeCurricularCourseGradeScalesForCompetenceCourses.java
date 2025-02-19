package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.CurricularCourse;
import org.fenixedu.academic.domain.GradeScale;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class ChangeCurricularCourseGradeScalesForCompetenceCourses extends CustomTask {

    @Override
    public void runTask() throws Exception {

        change("846654018158907"); // Atividades Extracurriculares I
        change("846654018158908"); // Atividades Extracurriculares II
//        changeCC("1127428915200404");
    }

    private void changeCC(final String id) {
        final CurricularCourse curricularCourse = FenixFramework.getDomainObject(id);
        taskLog("   %s = %s%n",
                curricularCourse.getDegreeCurricularPlan().getName(),
                curricularCourse.getGradeScale());
        curricularCourse.setGradeScale(GradeScale.TYPEAP);
    }

    private void change(final String id) {
        final CompetenceCourse competenceCourse = FenixFramework.getDomainObject(id);
        taskLog("%s%n", competenceCourse.getName());
        competenceCourse.getAssociatedCurricularCoursesSet().stream()
                .filter(cc -> cc.getGradeScale() != GradeScale.TYPEAP)
                .forEach(curricularCourse -> {
                    taskLog("   %s = %s%n",
                            curricularCourse.getDegreeCurricularPlan().getName(), curricularCourse.getGradeScale());
                    curricularCourse.setGradeScale(GradeScale.TYPEAP);
                });
    }

}