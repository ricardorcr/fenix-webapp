package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Optional;

public class FixStandaloneRegistrationProtocol extends CustomTask {

    RegistrationProtocol normal = null;

    @Override
    public void runTask() throws Exception {
        normal = FenixFramework.getDomainObject("5295694675969");
        AdmissionProcess admissionProcess = FenixFramework.getDomainObject("852907490541571"); //curriculares isoladas
        admissionProcess.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(this::hasBorded)
                .forEach(this::fix);
    }

    private boolean hasBorded(Application application) {
        if (application.getDataObject().has("outcomeState")) {
            final JsonObject outcomeState = application.getDataObject().get("outcomeState").getAsJsonObject();
            return "BOARDING".equals(outcomeState.get("id").getAsString());
        } else {
            return false;
        }
    }

    private void fix(Application application) {
        final Optional<Registration> optionalRegistration = application.getAccount().getUser().getPerson().getStudent().getRegistrationStream()
                .filter(r -> r.getDegreeType().isEmpty())
                .findAny();
        if (optionalRegistration.isPresent()) {
            final Registration registration = optionalRegistration.get();
            taskLog("Antes %s\t%s\t%s%n", registration.getExternalId(), registration.getNumber(), registration.getRegistrationProtocol().getCode());
            registration.setRegistrationProtocol(normal);
        } else {
            taskLog("Houston we have a problem!");
        }
    }
}
