package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.wizard.RemoteReader;
import org.fenixedu.admissions.util.DynamicForm;
import org.fenixedu.bennu.AdmissionsISTConfiguration;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.messaging.core.domain.Message;
import pt.ist.fenixframework.FenixFramework;

public class AppendRaidesForIncompleteAnswers extends CustomTask implements RemoteReader {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess dgesProcess = FenixFramework.getDomainObject("571432513831074");
        final JsonObject outcomeConfigJson = dgesProcess.getOutcomeConfigJson();
        final JsonObject forms = outcomeConfigJson.get("forms").getAsJsonObject();

        final JsonObject dgesOutcome = object("dgesCnaesOutcome.json");
        final JsonObject beforeOutcome = dgesOutcome.get("forms").getAsJsonObject().get("beforeOutcome").getAsJsonObject();
        forms.add("afterOutcome", beforeOutcome);
        dgesProcess.setOutcomeConfig(outcomeConfigJson.toString());

        dgesProcess.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .forEach(this::process);
    }

    private void process(final Application application) {
        final JsonObject dataObject = application.getDataObject();
        if (dataObject.has("outcomeFormData")) {
            final JsonObject outcomeFormData = dataObject.get("outcomeFormData").getAsJsonObject();
            if (isIncomplete(application)) {
                taskLog("%s\tApp: %s is incomplete.%n", application.getAccount().getUsername(), application.getExternalId());
                final JsonObject outcomeState = dataObject.get("outcomeState").getAsJsonObject();
                outcomeState.addProperty("canEditPostOutcomeForm", true);
                outcomeState.add("action", BundleUtil.getLocalizedString(AdmissionsISTConfiguration.BUNDLE,
                        "label.onboarding.action.boarding.finish").json());
                application.setData(dataObject.toString());

                final Person person = application.getAccount().getUser().getPerson();
                final boolean female = person.isFemale();
                final String message = "Car" + (female ? "a" : "o") + " " + person.getName() + "," +
                        "\n\nVerificámos que não respondeu a todas as perguntas do inquérito aquando da sua matrícula. " +
                        "Estes dados são obrigatórios para comunicar ao ministério.\n" +
                        "Por favor aceda ao portal https://fenix.tecnico.ulisboa.pt/fenixedu-connect para completar a actividade em falta." +
                        "\n\n" +
                        "Os melhores cumprimentos," +
                        "\nA Equipa FenixEdu";

                Message.fromSystem()
                        .to(Group.users(person.getUser()))
                        .subject("Preenchimento de formulário de matrícula incompleto")
                        .textBody(message)
                        .send();
            }
        }
    }

    private boolean isIncomplete(final Application application) {
        final JsonObject dataObject = application.getDataObject();
        final JsonObject outcomeFormDataApp = dataObject.get("outcomeFormData").getAsJsonObject();
        final JsonObject beforeOutcomeAnswers = outcomeFormDataApp.get("beforeOutcome").getAsJsonObject();

        final AdmissionProcess admissionProcess = application.getAdmissionProcessTarget().getAdmissionProcess();
        final JsonObject beforeOutcomeProcess = admissionProcess.getBeforeOutcomeFormDataJson();
        final DynamicForm dynamicForm = new DynamicForm(beforeOutcomeProcess).withData(beforeOutcomeAnswers);
        return dynamicForm.filter(field -> !field.isRequired())
                .filter(field -> !field.getName().equals("qualificationStart") && !field.getName().equals("scholarshipInstitution")
                                    && !field.getName().equals("additionalInformation"))
                .anyMatch(field -> field.getData() == null || field.getData().isJsonNull());
    }
}
