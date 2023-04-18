package pt.ist.fenix.webapp;

import edu.emory.mathcs.backport.java.util.Arrays;
import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class FixEnrolmentsInBothRegistrations extends CustomTask {


    @Override
    public void runTask() throws Exception {
        final ExecutionSemester nextExecutionSemester = ExecutionSemester.readActualExecutionSemester().getNextExecutionPeriod();
        nextExecutionSemester.getEnrolmentsSet().stream()
                .map(CurriculumModule::getRegistration)
                .distinct()
                .filter(r -> r.getSourceRegistration() != null)
                .filter(r -> r.getSourceRegistration().isConcluded())
                .filter(r -> r.getDegree().isSecondCycle())
                .filter(r -> {
                    final CycleCurriculumGroup secondCycle = r.getSourceRegistration().getLastStudentCurricularPlan().getSecondCycle();
                    return secondCycle != null && secondCycle.hasEnrolment(nextExecutionSemester.getExecutionYear());
                })
                .forEach(this::fix);
    }

    private void fix(final Registration registration) {
        final Registration sourceRegistration = registration.getSourceRegistration();
        final String dcpName = registration.getDegreeCurricularPlanName();
        taskLog("Fixing %s\t%s\t%s%n", registration.getNumber(), dcpName, sourceRegistration.getDegreeCurricularPlanName());

        final ExecutionYear currentYear = ExecutionYear.readCurrentExecutionYear();
        final CycleCurriculumGroup oldSecondCycle = sourceRegistration.getLastStudentCurricularPlan().getSecondCycle();
        oldSecondCycle.getEnrolmentsBy(currentYear).stream()
            .forEach(enrolment -> moveEnrolment(enrolment, registration.getLastStudentCurricularPlan().getSecondCycle()));
        tryRemoveOldSecondCycle(oldSecondCycle);
        if (sourceRegistration.getEnrolments(currentYear).isEmpty()) {
            sourceRegistration.getRegistrationDataByExecutionYearSet().stream()
                    .filter(data -> data.getExecutionYear() == currentYear)
                    .forEach(data -> {
                        data.setEventTemplate(null);
                        data.delete();
                    });
        }

        sourceRegistration.getGratuityEventsFor(currentYear)
            .forEach(event -> {
                if (event.canBeCanceled()) {
                    taskLog("Vou cancelar dívida: %s\t%s\t%s%n", event.getExternalId(), event.getPerson().getUsername(), event.getDescription().toString());
                    event.cancel(User.findByUsername("ist24616").getPerson(), "Dívida já criada no mestrado " + dcpName);
                }
            });
    }

    private void moveEnrolment(final Enrolment enrolment, final CurriculumGroup parent) {
        final CurriculumModule child = parent.getChildCurriculumModule(enrolment.getDegreeModule());
        if (child != null && child.isEnrolment()) {
            final Enrolment childEnrolment = (Enrolment) child;
            if (childEnrolment.getExecutionPeriod() == enrolment.getExecutionPeriod()) {
                throw new DomainException("error.SeparationCyclesManagement.enrolment.should.not.exist.for.same.executionPeriod");
            }
        }

        final Registration registration = parent.getStudentCurricularPlan().getRegistration();
        enrolment.setCurriculumGroup(parent);

        for (final Attends attend : enrolment.getAttendsSet()) {
            if (!registration.attends(attend.getExecutionCourse())) {
                attend.setRegistration(registration);
            }
        }
    }

    private void tryRemoveOldSecondCycle(final CycleCurriculumGroup oldSecondCycle) {
        if (canRemoveOldSecondCycle(oldSecondCycle)) {
            deleteCurriculumModules(oldSecondCycle);
        }
    }

    protected void deleteCurriculumModules(final CurriculumModule curriculumModule) {
        if (curriculumModule == null) {
            return;
        }
        if (!curriculumModule.isLeaf()) {
            final CurriculumGroup curriculumGroup = (CurriculumGroup) curriculumModule;
            for (; !curriculumGroup.getCurriculumModulesSet().isEmpty();) {
                deleteCurriculumModules(curriculumGroup.getCurriculumModulesSet().iterator().next());
            }
            curriculumGroup.delete();
        } else if (curriculumModule.isDismissal()) {
            curriculumModule.delete();
        } else {
            throw new DomainException("error.can.only.remove.groups.and.dismissals");
        }
    }

    private boolean canRemoveOldSecondCycle(final CycleCurriculumGroup oldSecondCycle) {
        for (final CurriculumLine curriculumLine : oldSecondCycle.getAllCurriculumLines()) {
            if (curriculumLine.isEnrolment() || curriculumLine.isDismissal()) {
                return false;
            } else if (!curriculumLine.isDismissal()) {
                throw new DomainException("error.unknown.curriculum.line");
            }
        }
        return true;
    }
}
