package pt.ist.fenix.webapp.task.paperPusher.academic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.bennu.core.json.ImmutableJsonElement;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.dynamicForms.LocalizedStringBuilder;
import org.fenixedu.smartFlow.domain.Flow;
import org.fenixedu.smartFlow.domain.SmartFlowSystem;
import pt.ist.fenixframework.FenixFramework;

public class InjectCancelStep extends CustomTask implements LocalizedStringBuilder {

    @Override
    public void runTask() throws Exception {
        SmartFlowSystem.getInstance().getFlowTemplateSet().stream()
                .filter(flowTemplate -> flowTemplate.getName().equals("student-registration-flow"))
                .flatMap(flowTemplate -> flowTemplate.getFlowSet().stream())
                .forEach(this::inject);
    }

    private void inject(final Flow flow) {
        FenixFramework.atomic(() -> {
            final JsonObject data = flow.getData().get();
            JsonArray actionNodes = data.getAsJsonObject("config").getAsJsonArray("actionNodes");
            final JsonElement[] toRemove = new JsonElement[1];
            actionNodes.forEach(jsonElement -> {
                if (jsonElement.getAsJsonObject().get("id").getAsString().equals("cancel")) {
                    toRemove[0] = jsonElement;
                }
            });
            actionNodes.remove(toRemove[0]);
            actionNodes
                    .add(JsonUtils.toJson(node -> {
                        node.addProperty("id", "cancel");
                        node.addProperty("type", "generic");
                        node.addProperty("comment", false);
                        node.add("title", ls("Cancelado", "Cancelled").json());
                        node.add("description", ls("Cancelado", "Cancelled").json());
                        node.add("actions", JsonUtils.toJson(actions -> {
                            actions.add("CANCEL", JsonUtils.toJson(cancel -> {
                                cancel.addProperty("primary", true);
                                cancel.addProperty("to", "FAILURE");
                                cancel.add("label", ls("Cancelar", "Cancel").json());
                            }));
                        }));
                        node.addProperty("queue", "academic-services-document-confirmation");
                    }));
            flow.setData(ImmutableJsonElement.of(data));
        });
    }

}