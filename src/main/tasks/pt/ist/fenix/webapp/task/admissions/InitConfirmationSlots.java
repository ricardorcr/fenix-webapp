package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.RegistrationProcessState;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.admissions.ist.service.RegistrationService;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.queueing.domain.AttendanceQueue;
import pt.ist.fenixframework.FenixFramework;

public class InitConfirmationSlots extends WriteCustomTask {

    int slots = 0;

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(this::needToApplySurvey)
                .flatMap(admissionProcess -> admissionProcess.getAdmissionProcessTargetSet().stream())
                .filter(this::init)
                .filter(this::cycle)
                .flatMap(admissionProcessTarget -> admissionProcessTarget.getApplicationSet().stream())
                .filter(application -> application.getLockInstant() != null)
                .filter(application -> Utils.registrationFor(application) != null)
                .filter(application -> Utils.outcomeStateFor(application) == RegistrationProcessState.REGISTERED)
                .filter(application -> needsSlot(application))
                .forEach(application -> {
                    final AdmissionProcessTarget admissionProcessTarget = application.getAdmissionProcessTarget();
                    final JsonObject config = admissionProcessTarget.getOutcomeConfigJson();
                    final Degree degree = FenixFramework.getDomainObject(config.get("degree").getAsString());
                    final String queueID = degree.getCurrentCampus().iterator().next().getName().indexOf("agus") >= 0 ?
                            "853070699298819" : "853070699298818";
                    slots++;
                    if (queueID == null) {
                        throw new Error("No cycle for: " + admissionProcessTarget.getExternalId()
                                + " " + admissionProcessTarget.getAdmissionProcess().getExternalId());
                    }
                    final AttendanceQueue queue = FenixFramework.getDomainObject(queueID);
                    if (queue == null || queue.getAttendanceSlotSet().isEmpty()) {
                        throw new Error("Queue has no slots");
                    }

                    RegistrationService.addToConfirmationQueueIfNeeded(application);
                    slots++;
                });

        taskLog("Need %s slots%n", slots);

        throw new Error("Abort TX");
    }

    private boolean needsSlot(final Application application) {
        boolean isAlreadyStudent = false;
        final Student student = application.getAccount().getUser().getPerson().getStudent();
        if (student != null) {
            final Registration registration = Utils.registrationFor(application);
            isAlreadyStudent = student.getRegistrationStream()
                    .filter(r -> r != registration)
                    .findAny().isPresent();
        }
        return !isAlreadyStudent;
    }

    private boolean cycle(final AdmissionProcessTarget target) {
        final JsonObject config = target.getOutcomeConfigJson();
        final CycleType cycleType = cycleTypeFor(config);
        return cycleType == CycleType.FIRST_CYCLE;
    }

    private boolean init(final AdmissionProcessTarget admissionProcessTarget) {
        final JsonObject config = admissionProcessTarget.getOutcomeConfigJson();
        final Degree degree = FenixFramework.getDomainObject(config.get("degree").getAsString());
        final String queueID = degree.getCurrentCampus().iterator().next().getName().indexOf("agus") >= 0 ?
                "853070699298819" : "853070699298818";
        if (queueID == null) {
            throw new Error("No cycle for: " + admissionProcessTarget.getExternalId()
                    + " " + admissionProcessTarget.getAdmissionProcess().getExternalId());
        }
        final AttendanceQueue queue = FenixFramework.getDomainObject(queueID);
        if (queue == null || queue.getAttendanceSlotSet().isEmpty()) {
            throw new Error("Queue has no slots");
        }
        config.addProperty("confirmationDocumentQueue", queue.getExternalId());
        return true;
    }

    private boolean needToApplySurvey(final AdmissionProcess admissionProcess) {
        return admissionProcess.getTitle().getContent().indexOf("2023") > 0
                && Utils.needsDocumentConfirmation(admissionProcess);
    }

    private CycleType cycleTypeFor(final JsonObject config) {
        final String cycleType = JsonUtils.get(config, "cycleType");
        return cycleType == null ? null : CycleType.valueOf(cycleType);
    }

}