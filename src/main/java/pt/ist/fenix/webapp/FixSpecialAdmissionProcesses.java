package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class FixSpecialAdmissionProcesses extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess forcaAerea = FenixFramework.getDomainObject("852907490541573");
        set(forcaAerea, "5295694675970", "1415058580045825");
        final AdmissionProcess militar = FenixFramework.getDomainObject("852907490541572");
        set(militar, "5295694675975", "1415058580045825");
        final AdmissionProcess azores = FenixFramework.getDomainObject("852907490541575");
        set(azores, "5295694675969", "7683696492557");
        final AdmissionProcess special = FenixFramework.getDomainObject("852907490541574");
        set(special, "5295694675969", null);
    }

    private void set(final AdmissionProcess process, final String protocolId, final String ingressionTypeId) {
        process.getAdmissionProcessTargetSet().stream()
                .forEach(target -> {
                    final JsonObject config = target.getOutcomeConfigJson();
                    config.addProperty("protocol", protocolId);
                    if (ingressionTypeId != null) {
                        config.addProperty("ingressionType", ingressionTypeId);
                    }
                    String outcome = config.toString().replace("Reingressar", "Ingressar");
                    outcome = outcome.replace("ReEnroll", "Enroll");
                    target.setOutcomeConfig(outcome);
                });
    }

}