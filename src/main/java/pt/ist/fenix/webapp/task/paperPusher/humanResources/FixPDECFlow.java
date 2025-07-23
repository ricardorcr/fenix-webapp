package pt.ist.fenix.webapp.task.paperPusher.humanResources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.admissions.util.RemoteReader;
import org.fenixedu.bennu.core.json.ImmutableJsonElement;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.smartFlow.domain.FlowTemplate;
import org.jetbrains.annotations.Nullable;
import pt.ist.fenixframework.FenixFramework;

public class FixPDECFlow extends CustomTask implements RemoteReader {

    @Override
    public void runTask() throws Exception {
        final JsonObject data = object("forms/humanResources/SpeciallyHiredFacultyContractFormAndFlow.json");
        final JsonObject templateJson = data.getAsJsonObject("flowTemplate");
        final JsonObject config = templateJson.getAsJsonObject("config");
        final JsonArray forms = config.get("forms").getAsJsonArray();
        final JsonObject formToAdd = getForm(forms, "review-documents-form");

        final FlowTemplate flowTemplate = FenixFramework.getDomainObject("1979387217969156");
        flowTemplate.setConfig(new ImmutableJsonElement<>(config));

        flowTemplate.getFlowSet()
                .forEach(flow -> {
                    final JsonObject flowData = flow.getData().get();
                    final JsonObject flowConfig = flowData.get("config").getAsJsonObject();
                    boolean hasChanges = false;
                    if (flowConfig.has("forms")) {
                        final JsonArray flowForms = flowConfig.get("forms").getAsJsonArray();
                        if (getForm(flowForms, "review-documents-form") == null) {
                            flowForms.add(formToAdd);
                            hasChanges = true;
                        }
                    }
                    if (flowConfig.has("actionNodes")) {
                        final JsonArray flowActionNodes = flowConfig.get("actionNodes").getAsJsonArray();
                        for (JsonElement element : flowActionNodes) {
                            final JsonObject actionNode = element.getAsJsonObject();
                            if (actionNode.get("id").getAsString().equals("step-5-request-document-changes")) {
                                final JsonObject button =
                                        actionNode.get("buttons").getAsJsonArray().get(0).getAsJsonObject();
                                final JsonObject handlers = button.get("handlers").getAsJsonObject();
                                handlers.addProperty("action", "SUBMIT");
                                hasChanges = true;
                            }
                        }
                    }
                    if (hasChanges) {
                        flow.setData(new ImmutableJsonElement<>(flowData));
                    }
                });
    }

    private @Nullable JsonObject getForm(final JsonArray forms, final String formId) {
        JsonObject formToAdd = null;
        for (JsonElement form : forms) {
            JsonObject jsonForm = form.getAsJsonObject();
            if (jsonForm.get("id").getAsString().equals(formId)) {
                formToAdd = jsonForm;
                break;
            }
        }
        return formToAdd;
    }

    @Override
    public String baseUrl() {
        return "https://repo.dsi.tecnico.ulisboa.pt/fenixedu/data/-/raw/master/paperPusher/";
    }
}
