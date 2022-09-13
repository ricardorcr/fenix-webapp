package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.RegistrationProcessState;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.admissions.ist.service.RegistrationService;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class AddToConfirmationQueueIfNeeded extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .flatMap(ap -> ap.getAdmissionProcessTargetSet().stream())
                .filter(target -> {
                    if (target.getOutcomeConfigJson() != null) {
                        final ExecutionYear executionYear = Utils.yearFor(target);
                        return executionYear != null && executionYear.getName().contains("2023");
                    } else {
                        return false;
                    }
                })
                .filter(target -> target.getOutcomeConfigJson().get("confirmationDocumentQueue") != null)
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(app -> Utils.registrationFor(app) != null)
                .forEach(this::checkNeededChangeOutcomeState);
    }

    private void checkNeededChangeOutcomeState(final Application application) {
        FenixFramework.atomic(() -> {
            final Enum outcomeState = Utils.outcomeStateFor(application);
            if (outcomeState == RegistrationProcessState.REGISTERED) {
                final AdmissionProcess admissionProcess = application.getAdmissionProcessTarget().getAdmissionProcess();
                if (Utils.isToChangeOutcomeState(admissionProcess)) {
                    if (Utils.needsDocumentConfirmation(admissionProcess)) {
                        if (!Utils.hasConfirmationDocumentAttendanceSlot(application)) {
                            taskLog("Add slot for: %s - %s%n", application.getExternalId(), application.getAdmissionProcessTarget().getAdmissionProcess().getTitle().getContent());
                            RegistrationService.addToConfirmationQueueIfNeeded(application);
                        }
                    }
                }
            }
        });
    }

}