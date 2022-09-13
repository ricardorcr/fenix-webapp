package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class FixUcranianAdmissionsOnboarding extends CustomTask {
    @Override
    public void runTask() throws Exception {
        AdmissionProcess admissionProcess = FenixFramework.getDomainObject("571432513831065");
        admissionProcess.getAdmissionProcessTargetSet().stream()
                .forEach(target -> {
                    final JsonObject outcomeConfigJson = target.getOutcomeConfigJson();
                    outcomeConfigJson.addProperty("eventTemplateForFirstYear", "571836240756817"); //same as normal
                    target.setOutcomeConfig(outcomeConfigJson.toString());
                });
    }
}
