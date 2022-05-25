package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kong.unirest.Unirest;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.ist.wizard.RemoteReader;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;

import java.util.Locale;

public class SetupAllTestForms extends CustomTask implements RemoteReader {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().forEach(admissionProcess -> {
            admissionProcess.setFormData(admissionProcess.getFormData().replaceAll("https://fenix.tecnico.ulisboa.pt", CoreConfiguration.getConfiguration().applicationUrl()));
            admissionProcess.setOutcomeConfig(admissionProcess.getOutcomeConfig().replaceAll("https://fenix.tecnico.ulisboa.pt", CoreConfiguration.getConfiguration().applicationUrl()));
        });

        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(admissionProcess -> admissionProcess.getTags().getContent().startsWith("Demo Form"))
                .forEach(admissionProcess -> admissionProcess.delete());

        final String raidesForm = string("raidesForm.json");
        final String mobilityRaidesForm = string("mobilityRaidesForm.json");

        setup("Segundo Ciclo - Form Candidatura",  string("2ndCycleFormData.json"));
        setup("Segundo Ciclo - Form Matrícula", raidesForm);

        setup("Internacionais - Form Candidatura", string("internationalFormData.json"));
        setup("Internacionais - Form Matrícula", raidesForm);

        setup("Maiores 23 - Form Candidatura", string("above23FormData.json"));
        setup("Maiores 23 - Form Matrícula", raidesForm);

        setup("Mudança de Curso - Form Candidatura", string("degreeChangeFormData.json"));
        setup("Mudança de Curso - Form Matrícula", raidesForm);

        setup("DGES - Form Matrícula", string("dgesCnaesOutcome.json"));

        setup("Cursos Médios e Superiores - Form Candidatura", string("middleAndHigherEducationFormData.json"));
        setup("Cursos Médios e Superiores - Form Matrícula", raidesForm);

        setup("Unidades Curriculares Isoladas - Form Candidatura", string("individualCurricularCourseFormData.json"));

        setup("Reingresso - Form Candidatura", string("reingressionFormData.json"));
        setup("Reingresso - Form Matrícula", raidesForm);

        setup("Regime Especial - Academia Militar - Form Candidatura", string("specialRegimentMilitaryFormData.json"));
        setup("Regime Especial - Academia Militar - Form Matrícula", raidesForm);

        setup("Regime Especial - Força Aérea - Form Candidatura", string("specialRegimentMilitaryFormData.json"));
        setup("Regime Especial - Força Aérea - Form Matrícula", raidesForm);

        setup("Regime Especial - Form Candidatura", string("specialRegimentFormData.json"));
        setup("Regime Especial - Form Matrícula", raidesForm);

        setup("Mobilidade Normal Simples - Form Candidatura", string("mobilityInboundFormData.json"));
        setup("Mobilidade Normal Simples - Form Matrícula", mobilityRaidesForm);
        setup("Mobilidade Normal Simples - Form Pós-Matrícula", afterOutcome("mobilityInboundOutcomeConfig.json"));

        setup("Mobilidade Duplo Grau - Form Candidatura", string("mobilityInboundFormData.json"));
        setup("Mobilidade Duplo Grau - Form Matrícula", raidesForm);
        setup("Mobilidade Duplo Grau - Form Pós-Matrícula", afterOutcome("mobilityInboundJointProgrammesOutcomeConfig.json"));

        setup("Mobilidade Joint Program - Form Candidatura", string("mobilityInboundJointProgrammesFormData.json"));
        setup("Mobilidade Joint Program - Form Matrícula", raidesForm);
        setup("Mobilidade Joint Program - Form Pós-Matrícula", afterOutcome("mobilityInboundJointProgrammesOutcomeConfig.json"));

        setup("Creditação Actividades Extra-Curriculares", string("CreditExtraCurricularActivitesFormData.json"));
    }

    private void setup(String name, String form) {
        final DateTime startDate = new DateTime();
        final DateTime endDate = startDate.plusDays(30);
        final String informationUrl = "https://tecnico.ulisboa.pt";
        final String email = "noreply@tecnico.ulisboa.pt";
        final LocalizedString title = ls(name, name);
        final LocalizedString tags = ls("Demo Formulários", "Demo Forms");

        final AdmissionProcess admissionProcess = AdmissionProcess.createAdmissionProcess(title, startDate, endDate, informationUrl, tags, email);

        final JsonObject outcome = new JsonObject();
        outcome.add("type", new JsonObject());
        outcome.getAsJsonObject("type").addProperty("name", "demo");
        outcome.getAsJsonObject("type").add("title", ls("Demo", "Demo").json());
        outcome.getAsJsonObject("type").add("answer", ls("Quero experimentar os formulários", "I want to try out the forms").json());
        admissionProcess.setOutcomeConfig(outcome.toString());

        admissionProcess.setFormData(form.replace("{admissionProcess}", admissionProcess.getExternalId()));

        admissionProcess.createAdmissionProcessTarget(ls(name, name), null);
    }

    private LocalizedString ls(final String pt, final String en) {
        return new LocalizedString(Locale.forLanguageTag("pt-PT"), pt).with(Locale.forLanguageTag("en-GB"), en);
    }

    @Override
    public String string(final String filename) {
        return Unirest.get(RemoteReader.BASE_URL + filename).asString().getBody()
                .replaceAll("https://fenix.tecnico.ulisboa.pt", CoreConfiguration.getConfiguration().applicationUrl());
    }

    public String afterOutcome(final String filename) {
        final JsonObject form = new JsonParser().parse(string(filename)).getAsJsonObject();
        return form.getAsJsonObject("forms").getAsJsonObject("afterOutcome").toString();
    }

}
