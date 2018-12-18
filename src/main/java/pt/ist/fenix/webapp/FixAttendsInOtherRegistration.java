package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class FixAttendsInOtherRegistration extends CustomTask {

    @Override
    public void runTask() throws Exception {
        ExecutionSemester executionSemester = ExecutionSemester.readActualExecutionSemester();
        executionSemester.getAssociatedExecutionCoursesSet().stream()
                .flatMap(es -> es.getAttendsSet().stream())
                .filter(at -> at.getEnrolment() != null && at.getRegistration() != at.getEnrolment().getRegistration())
                .forEach(this::fix);
    }

    private void fix(final Attends attends) {
        taskLog("original: %s\tcorrecta: %s\t%s\t%s%n", attends.getRegistration().getDegreeCurricularPlanName(),
                attends.getEnrolment().getRegistration().getDegreeCurricularPlanName(),
                attends.getEnrolment().getRegistration().getNumber(),
                attends.getEnrolment().getCurricularCourse().getName());
        Registration correctRegistration = attends.getEnrolment().getRegistration();
        attends.setRegistration(correctRegistration);
    }
}
