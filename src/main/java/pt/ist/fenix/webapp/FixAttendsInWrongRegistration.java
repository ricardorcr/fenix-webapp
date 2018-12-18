package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.util.Optional;

public class FixAttendsInWrongRegistration extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getStudentsSet().stream()
                .forEach(this::fixIfNeeded);
    }

    private void fixIfNeeded(Student student) {
        if (student.getRegistrationsSet().size() > 1) {
            final ExecutionSemester currentPeriod = ExecutionSemester.readActualExecutionSemester();
            final Optional<Registration> firstCycleRegistration = student.getRegistrationsSet().stream()
                    .filter(r -> r.isConcluded())
                    .filter(r -> r.getDegree().isFirstCycle())
                    .filter(r -> !r.getAttendsForExecutionPeriod(currentPeriod).isEmpty())
                    .findAny();
            if (firstCycleRegistration.isPresent()) {
                final Optional<Registration> secondCycleRegistration = student.getRegistrationsSet().stream()
                        .filter(r -> r.getDegree().isSecondCycle())
                        .filter(r -> !r.getEnrolments(currentPeriod).isEmpty())
                        .filter(r -> r.getLastState().getStateType().canHaveCurriculumLinesOnCreation())
                        .findAny();
                if (secondCycleRegistration.isPresent() && secondCycleRegistration.get() != firstCycleRegistration.get()) {
                    taskLog("Student %s 1st %s - 2nd %s%n", student.getNumber(), firstCycleRegistration.get().getDegreeCurricularPlanName(),
                            secondCycleRegistration.get().getDegreeCurricularPlanName());
                    firstCycleRegistration.get().getAttendsForExecutionPeriod(currentPeriod).stream()
                            .filter(at -> secondCycleRegistration.get().getEnrolments(currentPeriod).contains(at.getEnrolment()))
                            .forEach(at -> {
                                taskLog("\t%s%n", at.getEnrolment().getCurricularCourse().getName());
                                at.setRegistration(secondCycleRegistration.get());
                            });
                    taskLog("--------------------------------------------------");
                }
            }
        }
    }
}
