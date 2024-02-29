package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class FixEmptyRegistrationData extends CustomTask {

    @Override
    public void runTask() throws Exception {
        ExecutionYear.readCurrentExecutionYear().getRegistrationDataByExecutionYearSet().stream()
                .filter(rd -> rd.getEnrolmentDate() != null)
                .filter(rd -> rd.getRegistration().getStudentCurricularPlansSet().stream()
                        .noneMatch(scp -> scp.hasAnyCurriculumLines(rd.getExecutionYear())))
                .forEach(this::fix);
    }

    private void fix(RegistrationDataByExecutionYear registrationData) {
        taskLog("%s\t%s%n", registrationData.getRegistration().getNumber(), registrationData.getRegistration().getDegreeCurricularPlanName());
        final CycleCurriculumGroup secondCycle = registrationData.getRegistration().getLastStudentCurricularPlan().getSecondCycle();
        if (secondCycle != null && !secondCycle.hasAnyCurriculumLines()) {
            deleteCurriculumModules(secondCycle);
            taskLog("\t-- removed empty 2nd cycle");
        }
        registrationData.delete();
    }

    protected void deleteCurriculumModules(final CurriculumModule curriculumModule) {
        if (curriculumModule == null) {
            return;
        }
        if (!curriculumModule.isLeaf()) {
            final CurriculumGroup curriculumGroup = (CurriculumGroup) curriculumModule;
            for (; !curriculumGroup.getCurriculumModulesSet().isEmpty(); ) {
                deleteCurriculumModules(curriculumGroup.getCurriculumModulesSet().iterator().next());
            }
            curriculumGroup.delete();
        } else if (curriculumModule.isDismissal()) {
            curriculumModule.delete();
        } else {
            throw new DomainException("error.can.only.remove.groups.and.dismissals");
        }
    }
}
