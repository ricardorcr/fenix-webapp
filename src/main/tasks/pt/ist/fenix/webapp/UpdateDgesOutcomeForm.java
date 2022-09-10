package pt.ist.fenix.webapp;

import java.util.stream.Stream;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.template.AdmissionProcessTemplate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.fenixframework.FenixFramework;

public class UpdateDgesOutcomeForm extends CustomTask implements AdmissionProcessTemplate {

    @Override
    public void runTask() throws Exception {
        AdmissionProcess process = FenixFramework.getDomainObject("571432513831074");   
        process.setOutcomeConfig(appendTerms(string("dgesCnaesOutcome.json").replace("{admissionProcess}", process.getExternalId())));
    }

    private String appendTerms(final String s) {
        final JsonObject json = new JsonParser().parse(s).getAsJsonObject();
        final JsonObject forms = json.getAsJsonObject("forms");
        final JsonObject beforeOutcome = forms.getAsJsonObject("beforeOutcome");
        final JsonArray pages = beforeOutcome.getAsJsonArray("pages");
        pages.addAll(object("acceptTermsForm.json").getAsJsonArray("pages"));
        return json.toString();
    }

    @Override
    public String baseUrl() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonObject processForm() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonObject targetForm(AdmissionProcess process) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Stream<AdmissionProcess> createProcesses(JsonObject data) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Stream<AdmissionProcessTarget> createTargets(AdmissionProcess process, JsonObject data) {
        // TODO Auto-generated method stub
        return null;
    }

}