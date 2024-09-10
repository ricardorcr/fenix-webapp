package pt.ist.fenix.webapp.task.paperPusher.academic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.fenixedu.bennu.core.json.ImmutableJsonElement;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.dynamicForms.DynamicForm;
import org.fenixedu.smartFlow.domain.Flow;
import org.fenixedu.smartForms.domain.Request;
import org.fenixedu.smartForms.domain.SmartFormsSystem;
import pt.ist.fenixframework.FenixFramework;

public class CheckAndFixRegistryCodeCoherence extends CustomTask {
    @Override
    public void runTask() throws Exception {
        SmartFormsSystem.getInstance().getRequestTypeSet().stream()
                .filter(requestType -> requestType.getName().getContent().equals("Suplemento ao Diploma"))
                .flatMap(requestType -> requestType.getCurrentRequestTypeVersion().getRequestSet().stream())
                .filter(request -> request.getRegistryCode() == null)
                .forEach(request -> {
                    final JsonElement flowId = request.getData().get().get("flowId");
                    final JsonElement requestId = request.getData().get().get("requestId");
                    if (flowId != null) {
                        final Flow flow = FenixFramework.getDomainObject(flowId.getAsString());
                        final Request originRequest = FenixFramework.getDomainObject(flow.getData().get().get("requestId").getAsString());
                        if (originRequest.getRegistryCode() != null) {
                            request.setRegistryCode(originRequest.getRegistryCode());
                            updateOutcomeForm(request);
                        }
                    } else if (requestId != null) {
                        final Request originRequest = FenixFramework.getDomainObject(requestId.getAsString());
                        if (originRequest.getRegistryCode() != null) {
                            request.setRegistryCode(originRequest.getRegistryCode());
                            updateOutcomeForm(request);
                        }
                    } else {
                        taskLog("Sem flowId nem requestId: %s%n", request.getExternalId());
                    }
                });
    }

    private void updateOutcomeForm(final Request request) {
        final DynamicForm dynamicOutcomeForm = request.outcomeForm(true);
        if (dynamicOutcomeForm.get("REGISTRY_CODE") != null) {
            dynamicOutcomeForm.get("REGISTRY_CODE").withData(new JsonPrimitive(
                    request.getRegistryCode().getCode()));

            final JsonObject dataObject = request.getData().get();
            final DynamicForm dynamicForm = new DynamicForm(request.getRequestTypeVersion().getOutcomeForm().get())
                    .overwriteReadonly(true)
                    .withData(dataObject.getAsJsonObject("outcomeForm"))
                    .overwriteReadonly(true)
                    .withData(dynamicOutcomeForm.toDataJson());

            final JsonObject result = dynamicForm.toDataJson();
            dataObject.add("outcomeForm", result);
            request.setData(ImmutableJsonElement.of(dataObject));
            request.setKeywords();
        }
    }
}
