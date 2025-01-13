package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class UpdateOutboundMobilityText extends CustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(Utils::isOutboundMobilityType)
                .forEach(ap -> {
                    String replaced = ap.getOutcomeConfig()
                            .replace("Caso não encontra as unidades pretendidas deve submeter o formulário disponível" +
                                            " no seguinte link: http://localhost:8080/fenixedu-smart-forms/forms",
                                    "Caso não encontre as unidades pretendidas, deve contactar o Núcleo de Mobilidade" +
                                            " e Parcerias Internacionais (nmpi@tecnico.ulisboa.pt).");
                    replaced = replaced
                            .replace("If you are unable to find the course you want, fill out to following form: " +
                                            "http://localhost:8080/fenixedu-smart-forms/forms",
                                    "If you are unable to find the course you want, contact the International " +
                                            "Mobility and Partnerships Office (nmpi@tecnico.ulisboa.pt).");
                    ap.setOutcomeConfig(replaced);
                });
    }
}
