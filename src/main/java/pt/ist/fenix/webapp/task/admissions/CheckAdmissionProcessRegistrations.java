package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class CheckAdmissionProcessRegistrations extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess process = FenixFramework.getDomainObject("3104707304226830");
        process.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(app -> app.getDataObject().has("registration"))
                .forEach(app -> taskLog(app.getExternalId()));
    }
}
