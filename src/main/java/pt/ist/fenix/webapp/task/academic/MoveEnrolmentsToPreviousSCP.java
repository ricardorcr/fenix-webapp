package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.Credits;
import org.fenixedu.academic.domain.studentCurriculum.Dismissal;
import org.fenixedu.academic.domain.studentCurriculum.Substitution;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Set;

public class MoveEnrolmentsToPreviousSCP extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Registration registration = FenixFramework.getDomainObject("283734129520041");
        final StudentCurricularPlan originSCP = registration.getFirstStudentCurricularPlan();
        final StudentCurricularPlan newSCP = registration.getLastStudentCurricularPlan();
        originSCP.getCreditsSet().stream()
                .filter(Credits::isSubstitution)
                .map(Substitution.class::cast)
                .filter(substitution -> !substitution.getIEnrolments().iterator().next().getName().getContent().equals("Projeto Empresarial"))
                .forEach(substitution -> {
                    taskLog("##%s%n", substitution.getIEnrolments().iterator().next().getName().getContent());
                    substitution.getIEnrolments().stream()
                            .map(Enrolment.class::cast)
                            .forEach(enrolment -> move(enrolment, substitution));
                });
        newSCP.getEnrolmentsSet()
                .forEach(enrolment -> taskLog("humm: %s%n", enrolment.getName().getContent()));
        newSCP.delete();
    }

    private void move(final Enrolment enrolment, final Substitution substitution) {
        enrolment.setStudentCurricularPlan(substitution.getStudentCurricularPlan());
        final Set<Dismissal> dismissalsSet = substitution.getDismissalsSet();
        if (dismissalsSet.size() > 1) {
            throw new Error("No can do: " + substitution.getExternalId() + " " + substitution.getDescription());
        }
        enrolment.setCurriculumGroup(dismissalsSet.iterator().next().getCurriculumGroup());
        substitution.delete();
    }
}
