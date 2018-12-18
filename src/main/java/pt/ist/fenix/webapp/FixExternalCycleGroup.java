package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.ExternalCurriculumGroup;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class FixExternalCycleGroup extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getRegistrationsSet().stream()
                .flatMap(r -> r.getStudentCurricularPlansSet().stream())
                .filter(scp -> scp.getCycle(CycleType.SECOND_CYCLE) != null)
                .filter(scp -> scp.getCycle(CycleType.SECOND_CYCLE).getDegreeCurricularPlanOfDegreeModule() != scp.getDegreeCurricularPlan())
                .filter(scp -> !scp.getCycle(CycleType.SECOND_CYCLE).isExternal())
                .forEach(scp -> {
                    final CycleCurriculumGroup firstCurriculumGroup = scp.getCycle(CycleType.FIRST_CYCLE);
                    final CycleCurriculumGroup secondCurriculumGroup = scp.getCycle(CycleType.SECOND_CYCLE);
                    taskLog("Aluno %s\tSCP %s%n", scp.getRegistration().getNumber(), scp.getName());

                    if (firstCurriculumGroup != null) {
                        final ExternalCurriculumGroup externalCurriculumGroup = new ExternalCurriculumGroup();
                        externalCurriculumGroup.setCurriculumGroup(scp.getRoot());
                        externalCurriculumGroup.setDegreeModule(secondCurriculumGroup.getDegreeModule());
                        secondCurriculumGroup.getCurriculumModulesSet().stream()
                                .forEach(curriculumModule -> curriculumModule.setCurriculumGroup(externalCurriculumGroup));
                        secondCurriculumGroup.delete();
                    } else {
                        taskLog("Caso estranho: %s %s%n", scp.getRegistration().getNumber(), scp.getName());
                    }
                });
    }
}
