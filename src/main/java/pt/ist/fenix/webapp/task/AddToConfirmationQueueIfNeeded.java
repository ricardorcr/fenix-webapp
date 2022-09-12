package pt.ist.fenix.webapp.task;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.RegistrationProcessState;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.admissions.ist.service.RegistrationService;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import pt.ist.fenixframework.Atomic;

@Task(englishTitle = "Add to confirmation queue if needed", readOnly = true)
public class AddToConfirmationQueueIfNeeded extends CronTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(process -> isOutcomePeriodOpen(process, 7))
                .flatMap(ap -> ap.getAdmissionProcessTargetSet().stream())
                .filter(target -> {
                    if (target.getOutcomeConfigJson() != null) {
                        final ExecutionYear executionYear = Utils.yearFor(target);
                        return executionYear != null;
                    } else {
                        return false;
                    }
                })
                .filter(target -> target.getOutcomeConfigJson().get("confirmationDocumentQueue") != null)
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(app -> Utils.registrationFor(app) != null)
                .forEach(this::checkNeededChangeOutcomeState);
    }

    @Atomic
    private void checkNeededChangeOutcomeState(final Application application) {
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
    }

    public boolean isOutcomePeriodOpen(final AdmissionProcess admissionProcess, final int offset) {
        return admissionProcess.getStartOutcomePeriod() != null && admissionProcess.getEndOutcomePeriod() != null
                && admissionProcess.getStartOutcomePeriod().isBeforeNow() && admissionProcess.getEndOutcomePeriod().plusDays(offset).isAfterNow();
    }
}