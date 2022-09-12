package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsQueue;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.UserAccountInfo;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class AllocateCampusVisitSlot extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .flatMap(admissionProcess -> admissionProcess.getAdmissionProcessTargetSet().stream())
                .flatMap(target -> target.getApplicationSet().stream())
                .forEach(this::process);
    }

    private void process(final Application application) {
        try {
            FenixFramework.atomic(() -> {
                final AdmissionProcessTarget target = application.getAdmissionProcessTarget();
                final AdmissionsQueue admissionsQueue = target.getAfterOutcomeQueue();
                if (admissionsQueue != null && Utils.registrationFor(application) != null &&
                        UserAccountInfo.getSlotsNotFinished(application.getAccount()).stream()
                                .noneMatch(slot -> slot.getAttendanceQueue() == admissionsQueue.queue)) {
                    taskLog("%s : %s%n",
                            target.getAdmissionProcess().getTitle().getContent(),
                            application.getAccount().getEmail());
                    admissionsQueue.allocateSlotFor(application);
                }
            });
        } catch (final Throwable t) {
            taskLog("   failled slot allocation.");
        }
    }

}