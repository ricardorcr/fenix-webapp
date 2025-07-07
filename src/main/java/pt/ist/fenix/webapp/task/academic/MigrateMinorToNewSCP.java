package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.IEnrolment;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.studentCurriculum.Substitution;
import org.fenixedu.academic.dto.administrativeOffice.dismissal.DismissalBean;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MigrateMinorToNewSCP extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Registration registration = FenixFramework.getDomainObject("1409634036456237");
        final StudentCurricularPlan oldSCP = registration.getFirstStudentCurricularPlan();
        final StudentCurricularPlan newSCP = registration.getLastStudentCurricularPlan();

        Set<CurriculumGroup> minors = oldSCP.getAllCurriculumGroups().stream()
                .filter(cg -> cg.getDegreeModule() != null)
                .filter(cg -> cg.getDegreeModule().isRoot())
                .filter(cg -> cg.getDegreeModule().getDegree().getDegreeType().getMinor())
                .filter(CurriculumGroup::hasAnyApprovedCurriculumLines) //TODO e se tiver chumbado ou NA não se tem que abrir a caixa na mesma?
                .collect(Collectors.toSet());
        Optional<CurriculumGroup> minor = newSCP.getAllCurriculumGroups()
                .stream()
                .filter(cg -> cg.getDegreeModule() != null)
                .filter(cg -> cg.getDegreeModule().getName().equals("Minor"))
                .findAny();
        minor.ifPresent(curriculumGroup -> minors.forEach(originMinor -> {
            final CurriculumGroup minorGroup = new CurriculumGroup(curriculumGroup, originMinor.getDegreeModule());
            addSubGroups(minorGroup, originMinor.getCurriculumGroups());
            originMinor.getApprovedCurriculumLines()
                    .forEach(cl -> {
                        if (!cl.isDismissal()) {
                            DismissalBean.SelectedCurricularCourse course = new DismissalBean.
                                    SelectedCurricularCourse(cl.getCurricularCourse(), newSCP);
                            course.setCurriculumGroup(getGroup(newSCP, cl.getCurriculumGroup()));
                            final Substitution substitution =
                                    newSCP.createNewSubstitutionDismissal(null, null,
                                            List.of(course), List.of((IEnrolment) cl), null, ExecutionSemester.readActualExecutionSemester());
                        } else {
                            final Dismissal dismissal = (Dismissal) cl;
                            dismissal.setCurriculumGroup(minorGroup);
                        }
                    });
        }));
    }

    private CurriculumGroup getGroup(final StudentCurricularPlan scp, final CurriculumGroup curriculumGroup) {
        return scp.getAllCurriculumGroups().stream()
                .filter(group -> group.getName().getContent().equals(curriculumGroup.getName().getContent()))
                .findAny()
                .orElse(null);
    }

    private void addSubGroups(final CurriculumGroup group, Set<CurriculumGroup> curriculumGroups) {
        curriculumGroups.forEach( curriculumGroup -> {
            new CurriculumGroup(group, curriculumGroup.getDegreeModule());
            addSubGroups(curriculumGroup, curriculumGroup.getCurriculumGroups());
        });
    }
}
