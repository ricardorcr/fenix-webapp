package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionsQueue;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.ist.domain.UserAccountInfo;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

public class AllocateCampusVisitSlot extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .flatMap(admissionProcess -> admissionProcess.getAdmissionProcessTargetSet().stream())
                .forEach(target -> {
                    final AdmissionsQueue admissionsQueue = target.getAfterOutcomeQueue();
                    if (admissionsQueue != null) {
                        target.getApplicationSet().stream()
                                .filter(application -> Utils.registrationFor(application) != null)
                                .filter(application -> UserAccountInfo.getSlotsNotFinished(application.getAccount()).stream()
                                        .noneMatch(slot -> slot.getAttendanceQueue() == admissionsQueue.queue))
                                .forEach(application -> {
                                    taskLog("%s : %s%n",
                                            target.getAdmissionProcess().getTitle().getContent(),
                                            application.getAccount().getEmail());
                                });
                    }
                });
    }

}