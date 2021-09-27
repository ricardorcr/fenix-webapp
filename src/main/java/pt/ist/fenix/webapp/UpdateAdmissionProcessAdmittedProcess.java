package pt.ist.fenix.webapp;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.Locale;

public class UpdateAdmissionProcessAdmittedProcess extends CustomTask {

    private static String[] processTitles = new String[] {"Reingressos 2021/2022", "Ingressos Acordo Academia Força Aérea 2021/2022",
        "Ingressos Acordo Academia Militar 2021/2022", "Ingressos Convénio Universidade dos Açores 2021/2022",
        "Ingressos Regimes Especiais 2021/2022", "Candidaturas a Unidades Curriculares Isoladas 1º Semestre 2021/2022"};

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(this::isToChange)
                .forEach(process -> {
                    process.setGradeConfig(null);
                    process.setHasAdmissionGranted(true);
                });

        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(this::isToChange)
                .flatMap(process -> process.getAdmissionProcessTargetSet().stream())
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(application -> application.getAccepted() != null && application.getAccepted())
                .forEach(application -> application.accept());
    }

    private boolean isToChange(AdmissionProcess admissionProcess) {
        final String title = admissionProcess.getTitle().getContent(Locale.forLanguageTag("pt-PT"));
        return Arrays.stream(processTitles).anyMatch(processToChange -> processToChange.equals(title));
    }
}