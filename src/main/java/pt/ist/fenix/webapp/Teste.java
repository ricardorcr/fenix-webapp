package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.academic.domain.accounting.events.gratuity.EnrolmentGratuityEvent;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.NoCourseGroupCurriculumGroupType;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Collections;

public class Teste extends CustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(ap -> {
                    final JsonObject outcomeJson = ap.getOutcomeJson();
                    final String type = outcomeJson.get("type").getAsJsonObject().get("name").getAsString();
                    return type.equals("mobilityInboundDoubleDegree") || ap.getTitle().toString().contains("Internacionais");
                })
                .flatMap(ap -> ap.getAdmissionProcessTargetSet().stream().flatMap(apt -> apt.getApplicationSet().stream()))
                .filter(app -> app.getDataObject().has("registration"))
                .map(app -> (Registration) FenixFramework.getDomainObject(app.getDataObject().get("registration").getAsString()))
                .filter(registration -> !registration.getStudentCurricularPlansSet().iterator().next().getEnrolmentsSet().isEmpty())
                .forEach(this::cancelEnrolments);
        throw new Error("Dry run");
    }

    private void cancelEnrolments(Registration registration) {
        taskLog("%s %s%n", registration.getPerson().getUsername(), registration.getPerson().getName());
        final StudentCurricularPlan scp = registration.getStudentCurricularPlansSet().iterator().next();
        scp.getEnrolmentsSet().forEach(enrolment -> {
            enrolment.getStudentCurricularPlan().removeCurriculumModulesFromNoCourseGroupCurriculumGroup(
                        Collections.singletonList(enrolment), enrolment.getExecutionPeriod(), NoCourseGroupCurriculumGroupType.STANDALONE);
        });
        scp.getGratuityEventsSet().stream()
                .filter(event -> event.getEventType().equals(EventType.STANDALONE_PER_ENROLMENT_GRATUITY))
                .map(event -> (EnrolmentGratuityEvent) event)
                .filter(event -> event.getExecutionYear() == ExecutionYear.readCurrentExecutionYear().getNextExecutionYear())
                .forEach(event -> taskLog("Afinalllll"));
    }
}