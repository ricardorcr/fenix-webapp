package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Locale;

public class AddIngressionTypeToOutcomeConfig extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final IngressionType ingressionType = IngressionType.findIngressionTypeByCode("EINT").get(); //Internacionais
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(process -> {
                    final String title = process.getTitle().getContent(Locale.forLanguageTag("pt-PT"));
                    return title.contains("Internacionais");
                })
                .flatMap(process -> process.getAdmissionProcessTargetSet().stream())
                .forEach(target -> addIngressionType(target, ingressionType));

        final IngressionType degreeChangeIngressionType = IngressionType.findIngressionTypeByCode("MPIC").get(); //Mudança Par/Curso
        AdmissionProcess admissionProcess = FenixFramework.getDomainObject("289957537120269"); //mudança par/curso
        admissionProcess.getAdmissionProcessTargetSet().stream()
                .forEach(target -> addIngressionType(target, degreeChangeIngressionType));
    }

    private void addIngressionType(AdmissionProcessTarget target, IngressionType ingressionType) {
        final JsonObject outcomeConfigJson = target.getOutcomeConfigJson();
        outcomeConfigJson.addProperty("ingressionType", ingressionType.getExternalId());
        target.setOutcomeConfig(outcomeConfigJson.toString());
    }
}
