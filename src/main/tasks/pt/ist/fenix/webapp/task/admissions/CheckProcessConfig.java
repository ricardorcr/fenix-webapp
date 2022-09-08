package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

public class CheckProcessConfig extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(this::include)
                .forEach(admissionProcess -> {
                    final JsonObject config = admissionProcess.getOutcomeConfigJson();
                    final JsonElement needsDocumentsConfirmation = config.get("needsDocumentsConfirmation");
                    final JsonElement automaticEnrollment = config.get("automaticEnrollment");
                    final JsonElement tutorDistribution = config.get("tutorDistribution");
                    taskLog("%s: %s%n   needsDocumentsConfirmation: %s%n   automaticEnrollment: %s%n   tutorDistribution: %s%n",
                            admissionProcess.getExternalId(),
                            admissionProcess.getTitle().getContent(),
                            needsDocumentsConfirmation != null && needsDocumentsConfirmation.getAsBoolean(),
                            automaticEnrollment != null && automaticEnrollment.getAsBoolean(),
                            tutorDistribution != null && tutorDistribution.getAsBoolean());
                });
    }

    private boolean include(final AdmissionProcess admissionProcess) {
        return admissionProcess.getTitle().getContent().indexOf("2023") > 0
                && !Utils.isMinor(admissionProcess)
                && !Utils.isHACS(admissionProcess)
                && !Utils.isOutboundMobilityType(admissionProcess);
    }

}