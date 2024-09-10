package pt.ist.fenix.webapp.task.paperPusher.academic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.bennu.core.json.ImmutableJsonElement;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.smartForms.domain.Request;
import org.fenixedu.smartForms.domain.SmartFormsSystem;

import java.util.ArrayList;
import java.util.List;

public class FixRepeatedKeywords extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SmartFormsSystem.getInstance().getRequestTypeSet().stream()
                .filter(requestType -> requestType.getName().getContent().equals("Suplemento ao Diploma"))
                .flatMap(requestType -> requestType.getCurrentRequestTypeVersion().getRequestSet().stream())
                .filter(request -> request.getRegistryCode() != null)
                .forEach(this::fix);
    }

    private void fix(final Request request) {
        final JsonObject data = request.getData().get();
        if (data.has("fixedKeywords")) {
            final List<JsonElement> toRemove = new ArrayList<>();
            final JsonArray fixedKeywords = data.get("fixedKeywords").getAsJsonArray();
            fixedKeywords.forEach(element -> {
                final String ptString = element.getAsJsonObject().get("pt-PT").getAsString();
                if (ptString.equals(request.getRegistryCode().getCode())) {
                    toRemove.add(element);
                }
            });
            toRemove.forEach(fixedKeywords::remove);
        }
        if (data.has("keywords")) {
            final List<JsonElement> toRemove = new ArrayList<>();
            final JsonArray keywords = data.get("keywords").getAsJsonArray();
            final JsonElement[] toKeep = new JsonElement[1];
            keywords.forEach(element -> {
                final String ptString = element.getAsJsonObject().get("pt-PT").getAsString();
                if (ptString.equals(request.getRegistryCode().getCode())) {
                    toKeep[0] = element;
                    toRemove.add(element);
                }
            });
            toRemove.forEach(keywords::remove);
            keywords.add(toKeep[0]);
        }
        request.setData(ImmutableJsonElement.of(data));
    }
}
