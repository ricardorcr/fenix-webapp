package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroupFactory;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class FixMEFTOptionsSCPs extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getRegistrationsSet().stream()
                .flatMap(r -> r.getStudentCurricularPlansSet().stream())
                .filter(scp -> scp.hasCycleCurriculumGroup(CycleType.SECOND_CYCLE))
                .filter(scp -> "MEFT 2021".equals(scp.getCycle(CycleType.SECOND_CYCLE).getDegreeCurricularPlanOfDegreeModule().getName()))
                .forEach(this::fix);
    }

    private void fix(final StudentCurricularPlan scp) {
        final CurriculumGroup group1 = getCurriculumGroup(scp.getCycle(CycleType.SECOND_CYCLE), "Grupo 1 (Descontinuado)");
        final CurriculumGroup group2 = getCurriculumGroup(scp.getCycle(CycleType.SECOND_CYCLE), "Grupo 2 (Descontinuado)");

        if (group1 != null) {
            fixGroup(group1);
            group1.delete();
        }
        if (group2 != null) {
            fixGroup(group2);
            group2.delete();
        }

        CurriculumGroup areaPrincipal = getCurriculumGroup(scp.getCycle(CycleType.SECOND_CYCLE), "Área Principal");
        CurriculumGroup olr = getCurriculumGroup(scp.getCycle(CycleType.SECOND_CYCLE), "Opções Livres Regulares");
        if (olr == null) {
            final CourseGroup courseGroup = getCourseGroup(scp.getCycle(CycleType.SECOND_CYCLE), "Opções Livres Regulares");
            olr = CurriculumGroupFactory.createGroup(areaPrincipal, courseGroup);
        }
        if (canMove(areaPrincipal, olr)) {
            moveOptionsFromTo(areaPrincipal, olr);
        } else {
            taskLog("Aluno %s ficaria com créditos a mais no grupo Opções Livres Regulares%n", scp.getRegistration().getNumber());
        }
    }

    private boolean canMove(final CurriculumGroup areaPrincipal, final CurriculumGroup olr) {
        final Double optionCredits = areaPrincipal.getCurriculumLines().stream()
                .filter(curriculumLine -> curriculumLine.isOptional())
                .map(CurriculumLine::getEctsCredits)
                .reduce(0.0, Double::sum);
        return olr.getDegreeModule().getMaxEctsCredits() >= optionCredits;
    }

    private CourseGroup getCourseGroup(final CycleCurriculumGroup cycle, final String groupName) {
        return cycle.getDegreeCurricularPlanOfDegreeModule().getAllCoursesGroups().stream()
                .filter(cg -> cg.getName().equals(groupName))
                .findAny().get();
    }

    private void moveOptionsFromTo(final CurriculumGroup areaPrincipal, final CurriculumGroup olr) {
        areaPrincipal.getCurriculumLines().stream()
                .filter(curriculumLine -> curriculumLine.isOptional())
                .forEach(curriculumLine -> {
                    taskLog("%s\tMoved option %s to Opções Livres Regulares%n", areaPrincipal.getStudent().getNumber(), curriculumLine.getName().getContent());
                    curriculumLine.setCurriculumGroup(olr);
                });
    }

    private void fixGroup(final CurriculumGroup group) {
        if (group != null) {
            for (CurriculumLine curriculumLine : group.getChildCurriculumLines()) {
                taskLog("Student: %s\tChanged %s\tfrom %s\tto %s%n", group.getParentCycleCurriculumGroup().getStudent().getNumber(),
                        curriculumLine.getName().getContent(), group.getName().getContent(),
                        group.getCurriculumGroup().getName().getContent());
                curriculumLine.setCurriculumGroup(group.getCurriculumGroup());
            }
        }
    }

    private CurriculumGroup getCurriculumGroup(final CurriculumGroup group, final String groupName) {
        if (group.getName().getContent().equals(groupName)) {
            return group;
        } else {
            for (CurriculumGroup childGroup : group.getChildCurriculumGroups()) {
                final CurriculumGroup curriculumGroup = getCurriculumGroup(childGroup, groupName);
                if (curriculumGroup != null) {
                    return curriculumGroup;
                }
            }
        }
        return null;
    }
}
