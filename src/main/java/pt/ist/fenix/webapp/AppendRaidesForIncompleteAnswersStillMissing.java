package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.wizard.RemoteReader;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.dynamicForms.DynamicForm;
import pt.ist.fenixframework.FenixFramework;

public class AppendRaidesForIncompleteAnswersStillMissing extends CustomTask implements RemoteReader {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess dgesProcess = FenixFramework.getDomainObject("571432513831074");

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
//                final JsonObject outcomeState = dataObject.get("outcomeState").getAsJsonObject();
//                outcomeState.addProperty("canEditPostOutcomeForm", true);
//                outcomeState.add("action", BundleUtil.getLocalizedString(AdmissionsISTConfiguration.BUNDLE,
//                        "label.onboarding.action.boarding.finish").json());
//                application.setData(dataObject.toString());
            }
        }
    }

    private boolean isIncomplete(final Application application) {
        final JsonObject dataObject = application.getDataObject();
        final JsonObject outcomeFormDataApp = dataObject.get("outcomeFormData").getAsJsonObject();
        if (outcomeFormDataApp.has("afterOutcome")) {
            return false;
        }
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
