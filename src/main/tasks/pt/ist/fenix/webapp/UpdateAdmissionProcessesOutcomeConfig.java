package pt.ist.fenix.webapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.List;

public class UpdateAdmissionProcessesOutcomeConfig extends CustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(ap -> !ap.getArchived())
                .forEach(this::addConfig);

    }

    private void addConfig(final AdmissionProcess admissionProcess) {
        final JsonObject outcomeConfigJson = admissionProcess.getOutcomeConfigJson();
        if (Utils.isCurricularCourse(admissionProcess)) {
            outcomeConfigJson.addProperty("needsDocumentsConfirmation", true);
            outcomeConfigJson.addProperty("changeOutcomeState", true);
        } else if (Utils.isReinstatement(admissionProcess)) {
            outcomeConfigJson.addProperty("needsDocumentsConfirmation", false);
            outcomeConfigJson.addProperty("changeOutcomeState", true);
        } else if (Utils.isDges(admissionProcess)) {
            outcomeConfigJson.addProperty("needsDocumentsConfirmation", false);
            outcomeConfigJson.addProperty("changeOutcomeState", true);
            outcomeConfigJson.addProperty("automaticEnrollment", true);
            outcomeConfigJson.addProperty("tutorDistribution", true);
        } else if (Utils.isDegreeType(admissionProcess)) {
            outcomeConfigJson.addProperty("needsDocumentsConfirmation", true);
            outcomeConfigJson.addProperty("changeOutcomeState", true);
            if (admissionProcess.getTitle().getContent().contains("Maiores")) {
                outcomeConfigJson.addProperty("automaticEnrollment", true);
                outcomeConfigJson.addProperty("tutorDistribution", true);
            }
            if (admissionProcess.getTitle().getContent().contains("Internacionais")) {
                outcomeConfigJson.addProperty("automaticEnrollment", true);
                outcomeConfigJson.addProperty("tutorDistribution", true);
            }
            if (admissionProcess.getTitle().getContent().contains("Titulares")) {
                outcomeConfigJson.addProperty("automaticEnrollment", true);
                outcomeConfigJson.addProperty("tutorDistribution", true);
            }
        } else if (Utils.isDegreeSpecificRegimentType(admissionProcess)) {
            final AdmissionProcessTarget target = admissionProcess.getAdmissionProcessTargetSet().iterator().next();
            final String protocolID = target.getOutcomeConfigJson().get("protocol").getAsString();
            final RegistrationProtocol protocol = FenixFramework.getDomainObject(protocolID);
            final List<String> protocols = Arrays.asList("MA", "MAR", "AFA", "NORMAL");
            if (protocols.contains(protocol.getCode())) {
                outcomeConfigJson.addProperty("needsDocumentsConfirmation", false);
            } else {
                outcomeConfigJson.addProperty("needsDocumentsConfirmation", true);
            }
            outcomeConfigJson.addProperty("changeOutcomeState", true);
        }
        admissionProcess.setOutcomeConfig(outcomeConfigJson.toString());
    }
}