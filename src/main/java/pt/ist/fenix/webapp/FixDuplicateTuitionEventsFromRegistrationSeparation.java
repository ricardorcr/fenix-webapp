package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.CustomEvent;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.stream.Stream;

public class FixDuplicateTuitionEventsFromRegistrationSeparation extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
//        final ExecutionSemester previousExecutionPeriod = ExecutionSemester.readActualExecutionSemester().getPreviousExecutionPeriod();
//        previousExecutionPeriod.getEnrolmentsSet().stream()
//                .map(CurriculumModule::getRegistration)
//                .distinct()
//                .filter(r -> r.getDegree().isSecondCycle())
//                .filter(r -> r.getSourceRegistration() != null)
//                .filter(r -> r.getSourceRegistration().isConcluded())
//                .filter(r -> hasAnyApprovedEnrolments(previousExecutionPeriod, r.getSourceRegistration()))
//                .forEach(r -> fix(r, r.getSourceRegistration(), previousExecutionPeriod.getExecutionYear()));
//
        final ExecutionSemester currentExecutionPeriod = ExecutionSemester.readActualExecutionSemester();
        currentExecutionPeriod.getEnrolmentsSet().stream()
                .map(CurriculumModule::getRegistration)
                .distinct()
                .filter(r -> r.getDegree().isSecondCycle())
                .filter(r -> r.getSourceRegistration() != null)
                .filter(r -> r.getSourceRegistration().isConcluded())
                .filter(r -> !r.getSourceRegistration().hasAnyEnrolmentsIn(currentExecutionPeriod))
                .forEach(r -> fix(r.getSourceRegistration(), r, currentExecutionPeriod.getExecutionYear()));

//        previousExecutionPeriod.getEnrolmentsSet().stream()
//                .map(CurriculumModule::getRegistration)
//                .distinct()
//                .filter(r -> r.getDegree().isFirstCycle())
//                .filter(Registration::isConcluded)
//                .flatMap(r -> r.getStudent().getRegistrationsSet().stream()
//                                .filter(r1 -> r1 != r && r1.getSourceRegistration() == r))
//                .filter(r -> r.getSourceRegistration().getLastStudentCurricularPlan().getSecondCycle() != null &&
//                                r.getSourceRegistration().getLastStudentCurricularPlan().getSecondCycle().hasEnrolment(previousExecutionPeriod))
//                .forEach(r -> fix(r, r.getSourceRegistration(), previousExecutionPeriod.getExecutionYear()));
    }

    private void fix(final Registration registration, final Registration registrationWithRightEvent, final ExecutionYear executionYear) {
        FenixFramework.atomic(() -> {
            final String dcpName = registrationWithRightEvent.getDegreeCurricularPlanName();

            if (registration.getNumber() != 92513) {
                if (executionYear.isCurrent() && registration.getDegree().isFirstCycle()) {
                    deleteRegistrationData(registration, executionYear);
                }
//            if (registration.getNumber() == 90328) {
//                deleteRegistrationData(registration, executionYear);
//            }
                getGratuityEventsFor(registration, executionYear)
                        .forEach(event -> {
                            if (event.canBeCanceled()) {
                                taskLog("Dívida a cancelar: %s\t%s\t%s%n", event.getExternalId(), event.getPerson().getUsername(), event.getDescription().toString());
                                event.cancel(User.findByUsername("ist24616").getPerson(), "Dívida já criada em " + dcpName);
                            }
                        });
            }
        });
    }

    private void deleteRegistrationData(final Registration registration, final ExecutionYear executionYear) {
        registration.getRegistrationDataByExecutionYearSet().stream()
                .filter(rd -> rd.getExecutionYear() == executionYear)
                .forEach(RegistrationDataByExecutionYear::delete);
    }

    private Stream<CustomEvent> getGratuityEventsFor(final Registration registration, final ExecutionYear executionYear) {
        return registration.getPerson().getEventsSet().stream()
                .filter(CustomEvent.class::isInstance)
                .map(CustomEvent.class::cast)
                .filter(event -> !event.isCancelled())
                .filter(event -> EventTemplate.Type.TUITION.isType(event))
                .filter(event -> {
                    final JsonObject configObject = event.getConfigObject();
                    final ExecutionYear eventExecutionYear = JsonUtils.toDomainObject(configObject, "executionYear");
                    Registration eventRegistration = JsonUtils.toDomainObject(configObject, "registration");
                    if (eventRegistration == null) {
                        final RegistrationDataByExecutionYear dataByExecutionYear = JsonUtils.toDomainObject(configObject, "registrationDataByExecutionYear");
                        if (dataByExecutionYear != null && FenixFramework.isDomainObjectValid(dataByExecutionYear)) {
                            eventRegistration = dataByExecutionYear.getRegistration();
                        }
                        else {
                            if (event.canBeCanceled()) {
                                final String eventName = event.getDescription().toString();
                                if (eventName.contains(executionYear.getName()) && eventName.contains(registration.getDegree().getSigla())) {
                                    taskLog("Has no valid data registration: %s\t%s\t%s%n", event.getExternalId(), eventName, registration.getDegree().getSigla());
                                    return eventExecutionYear == executionYear;
                                }
                            }
                        }
                    }
                    return eventExecutionYear == executionYear && eventRegistration == registration;
                });
    }

    final public boolean hasAnyApprovedEnrolments(final ExecutionSemester executionSemester, final Registration registration) {
        for (final StudentCurricularPlan studentCurricularPlan : registration.getStudentCurricularPlansSet()) {
            for (final Enrolment enrolment : studentCurricularPlan.getEnrolmentsSet()) {
                if (enrolment.isApproved() && enrolment.getExecutionPeriod() == executionSemester) {
                    return true;
                }
            }
        }
        return false;
    }
}
