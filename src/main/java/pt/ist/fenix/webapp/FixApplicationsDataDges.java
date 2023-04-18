package pt.ist.fenix.webapp;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class FixApplicationsDataDges extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess admissionProcess = FenixFramework.getDomainObject("571432513831074");
        admissionProcess.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(app -> app.getData().contains("\"residenceCountry\":\"!\""))
                .forEach(app -> {
                    taskLog("App: %s with wrong data%n", app.getExternalId());
                    final String data = app.getData();
                    final String fixedData = data.replace("\"residenceCountry\":\"!\"",
                            "\"residenceCountry\":{\"value\":\"PT\",\"label\":{\"pt-PT\":\"Portugal\",\"en-GB\":\"Portugal\"}}");
                    app.setData(fixedData);
                });
    }
}
