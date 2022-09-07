package pt.ist.fenix.webapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumLine;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.util.predicates.AndPredicate;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.YearMonthDay;
import pt.ist.fenixWebFramework.renderers.utils.RenderUtils;
import pt.ist.fenixedu.integration.ui.struts.action.academicAdministration.UpdateAbandonStateBean;
import pt.ist.fenixframework.DomainObject;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class CheckShouldBeAbandoned extends CustomTask {

    private static final String RESOURCE_BUNDLE = "FENIXEDU_IST_INTEGRATION_RESOURCES";
  
    @Override
    public void runTask() throws Exception {

        final ExecutionSemester second2021_2022 = ExecutionYear.readCurrentExecutionYear().getLastExecutionPeriod();
        final Registration registration = FenixFramework.getDomainObject("283734129518171");
        final boolean result = evaluateAndCheckEnrolments(registration, second2021_2022);
        taskLog("Devia estar: %s%n", !result);

        final boolean processed = processStudent(registration.getActiveStudentCurricularPlan());
        taskLog("Processado com sucesso: %s%n", processed);
    }

    private ExecutionSemester getWhenToAbandon() {
        return ExecutionYear.readCurrentExecutionYear().getNextExecutionYear().getFirstExecutionPeriod();
    }

    private boolean evaluateAndCheckEnrolments(final Registration registration, final ExecutionSemester semester) {
        ExecutionSemester startExecutionSemester = ExecutionSemester.readByYearMonthDay(registration.getStartDate());
        if (registration.getStartExecutionYear().isBeforeOrEquals(semester.getExecutionYear())
                && startExecutionSemester.isBefore(semester) && !registration.hasStateType(semester, RegistrationStateType.MOBILITY)) {
            if (hasAnyCurriculumLines(registration, semester)) {
                return true;
            }
            if (registration.getStartExecutionYear().isBeforeOrEquals(semester.getPreviousExecutionPeriod().getExecutionYear())
                    && !registration.hasStateType(semester.getPreviousExecutionPeriod(), RegistrationStateType.MOBILITY)) {
                return hasAnyCurriculumLines(registration, semester.getPreviousExecutionPeriod());
            }
        }
        return true;
    }

    private boolean hasAnyCurriculumLines(final Registration registration, final ExecutionSemester semester) {
        final AndPredicate<CurriculumModule> andPredicate = new AndPredicate<>();
        andPredicate.add(new CurriculumModule.CurriculumModulePredicateByType(Enrolment.class));
        andPredicate.add(new CurriculumLinePredicateByExecutionSemester(semester));

        for (final StudentCurricularPlan studentCurricularPlan : registration.getStudentCurricularPlansSet()) {
            if (studentCurricularPlan.hasAnyCurriculumModules(andPredicate)) {
                return true;
            }
        }
        return false;
    }

    private class CurriculumLinePredicateByExecutionSemester implements Predicate<CurriculumModule> {

        private final ExecutionSemester semester;

        private CurriculumLinePredicateByExecutionSemester(final ExecutionSemester semester) {
            this.semester = semester;
        }

        @Override
        public boolean test(final CurriculumModule module) {
            if (!module.isCurriculumLine()) {
                return false;
            }
            if (module.isEnrolment()) {
                return ((Enrolment) module).isValid(semester);
            }
            return ((CurriculumLine) module).getExecutionPeriod().equals(semester);
        }
    }

    private boolean processStudent(StudentCurricularPlan studentCurricularPlan) {
        final ExecutionSemester startChecking = getWhenToAbandon().getPreviousExecutionPeriod();
        final Registration registration = studentCurricularPlan.getRegistration();
        RegistrationState lastRegistrationState =
                registration.getLastRegistrationState(getWhenToAbandon().getExecutionYear().getPreviousExecutionYear());

        if (hasValidRegistrationAgreement(registration) && registration.isDegreeAdministrativeOffice()
                && lastRegistrationState != null && lastRegistrationState.isActive()
                && !lastRegistrationState.getStateType().equals(RegistrationStateType.MOBILITY) && !registration.hasConcluded()) {
            taskLog("Primeiro If");
            if (registration.getStartExecutionYear().isBefore(getWhenToAbandon().getExecutionYear())
                    && !hasAnyEnrolmentInPeriodOrPrevious(registration, startChecking)) {
                taskLog("Segundo If");
                if (registration.hasStateType(getWhenToAbandon().getPreviousExecutionPeriod(),
                        RegistrationStateType.EXTERNAL_ABANDON)
                        || registration.hasStateType(getWhenToAbandon(), RegistrationStateType.EXTERNAL_ABANDON)) {
                    taskLog("Terceiro If");
                    return false;
                }
                final YearMonthDay now = new YearMonthDay();
                final RegistrationState state =
                        RegistrationState.createRegistrationState(registration, null, (now.isBefore(getWhenToAbandon()
                                .getBeginDateYearMonthDay()) ? getWhenToAbandon().getBeginDateYearMonthDay() : now)
                                .toDateTimeAtMidnight(), RegistrationStateType.EXTERNAL_ABANDON);

                state.setRemarks(RenderUtils.getFormatedResourceString(RESOURCE_BUNDLE,
                        "message.academicAdministration.abandonState.observations"));

                return true;
            }
        }
        taskLog("Não entrou");
        return false;
    }

    private boolean hasValidRegistrationAgreement(final Registration registration) {
        return registration.getRegistrationProtocol() == null
                || (!registration.getRegistrationProtocol().isMilitaryAgreement() && !registration.getRegistrationProtocol()
                .isMobilityAgreement());
    }

    private boolean hasAnyEnrolmentInPeriodOrPrevious(final Registration registration, final ExecutionSemester executionSemester) {
        return evaluateAndCheckEnrolments(registration, executionSemester);
    }
}