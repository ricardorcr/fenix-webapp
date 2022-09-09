package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.RegistrationProcessState;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.admissions.ist.service.RegistrationService;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class AttemptSetSlot extends WriteCustomTask {
    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(ap -> !Utils.isDges(ap))
                .flatMap(ap -> ap.getAdmissionProcessTargetSet().stream())
                .filter(target -> {
                    if (target.getOutcomeConfigJson() != null) {
                        final ExecutionYear executionYear = Utils.yearFor(target);
                        return executionYear != null && executionYear.getName().contains("2023");
                    } else {
                        return false;
                    }
                })
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(app -> Utils.registrationFor(app) != null)
                .forEach(application -> {
                    final Enum outcomeState = Utils.outcomeStateFor(application);
                    if (outcomeState == RegistrationProcessState.REGISTERED) {
                        final AdmissionProcess admissionProcess = application.getAdmissionProcessTarget().getAdmissionProcess();
                        if (Utils.needsDocumentConfirmation(admissionProcess)) {
                            if (Utils.hasConfirmationDocumentAttendanceSlot(application)) {
                                // ok
                            } else {
                                // Need to schedule student
                                taskLog("Need to schedule %s%n", application.getExternalId());
                                //RegistrationService.addToConfirmationQueueIfNeeded(application);
                            }
                        } else {
                        }
                    }
                });
    }
}