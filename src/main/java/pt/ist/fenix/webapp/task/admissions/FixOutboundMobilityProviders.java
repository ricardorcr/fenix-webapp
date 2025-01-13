package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class FixOutboundMobilityProviders extends CustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(Utils::isOutboundMobilityType)
                .forEach(ap -> {
                    String replaced = ap.getOutcomeConfig().replace("{admissionProcess}", ap.getExternalId());
                    ap.setOutcomeConfig(replaced);
                });
    }
}
