package pt.ist.fenix.webapp;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UpdateAdmissionProcessEmail extends CustomTask {

    private static final Locale EN = new Locale("en", "GB");

    @Override
    public void runTask() throws Exception {
        Map<String, String> protocolEmails = new HashMap<>();
        protocolEmails.put("Cluster","erasmus@tecnico.ulisboa.pt");
        protocolEmails.put("Double Degree Brazil","outsideeurope@tecnico.ulisboa.pt");
        protocolEmails.put("Double Degree France","erasmus@tecnico.ulisboa.pt");
        protocolEmails.put("Double Degree Italy","erasmus@tecnico.ulisboa.pt");
        protocolEmails.put("Double Degree Spain","erasmus@tecnico.ulisboa.pt");
        protocolEmails.put("Double Degree USA","outsideeurope@tecnico.ulisboa.pt");
        protocolEmails.put("Erasmus Mundus","erasmusmundus@tecnico.ulisboa.pt");
        protocolEmails.put("Time","erasmus@tecnico.ulisboa.pt");

        protocolEmails.entrySet().forEach((entry) -> {
            AdmissionProcess admissionProcess = getAdmissionProcess(entry.getKey());
            admissionProcess.setSupportEmail(entry.getValue());
        });
    }

    private AdmissionProcess getAdmissionProcess(String protocol) {
        return AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(ap -> ap.getTitle().getContent(EN).equals(protocol))
                .findAny().get();
    }
}
