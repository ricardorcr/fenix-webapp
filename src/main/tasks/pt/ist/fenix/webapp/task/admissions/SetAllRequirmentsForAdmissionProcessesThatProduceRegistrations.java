package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonObject;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;

public class SetAllRequirmentsForAdmissionProcessesThatProduceRegistrations extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(Utils::isDegreeType)
                .forEach(admissionProcess -> {
                    final JsonObject data = admissionProcess.getFormDataJson();
                    JsonObject checks = data.getAsJsonObject("checks");
                    if (checks == null || checks.isJsonNull()) {
                        checks = new JsonObject();
                        data.add("checks", checks);
                    }
                    checks.addProperty("requirePersonalInformation", true);
                    checks.addProperty("requirePhotograph", true);
                    checks.addProperty("requireTaxInformation", true);
                    checks.addProperty("requireIdentity", true);

                    admissionProcess.setFormData(data.toString());
                });
    }

}