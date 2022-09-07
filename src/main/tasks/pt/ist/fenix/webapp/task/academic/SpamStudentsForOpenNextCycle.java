package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;

public class SpamStudentsForOpenNextCycle extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        ExecutionYear.readCurrentExecutionYear().getExecutionPeriodsSet().stream()
                .flatMap(p -> p.getEnrolmentsSet().stream())
                .map(e -> e.getStudentCurricularPlan())
                .filter(this::consider)
                .forEach(scp -> {
                    taskLog("%s%n", scp.getRegistration().getPerson().getUsername());
                });
                ;
    }

    private boolean consider(final StudentCurricularPlan studentCurricularPlan) {
        final CycleCurriculumGroup first = studentCurricularPlan.getCycle(CycleType.FIRST_CYCLE);
        if (first != null && first.isConcluded()) {
            if (studentCurricularPlan.getCycle(CycleType.SECOND_CYCLE) == null) {
                return studentCurricularPlan.getRegistration().getStudent().getRegistrationsSet().stream()
                        .filter(r -> r != studentCurricularPlan.getRegistration())
                        .noneMatch(r -> r.getStartExecutionYear().isCurrent() || r.getStartExecutionYear().getPreviousExecutionYear().isCurrent());
            }
        }
        return false;
    }

}