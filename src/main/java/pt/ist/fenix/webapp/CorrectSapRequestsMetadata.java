package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixframework.FenixFramework;

public class CorrectSapRequestsMetadata extends CustomTask {

    @Override
    public void runTask() throws Exception {

        SapRequest sapRequest = FenixFramework.getDomainObject("289708429235315");
        fix(sapRequest);
        sapRequest = FenixFramework.getDomainObject("289708429240620");
        fix(sapRequest);
    }

    private void fix(SapRequest sr) {
        JsonObject json = new JsonParser().parse(sr.getRequest()).getAsJsonObject();
        JsonObject workingDocument = json.get("workingDocument").getAsJsonObject();

        String metadata = workingDocument.get("metadata").getAsString();
        String stripMetadata = metadata.replace("\\", "");
        JsonObject metadataJson = new JsonParser().parse(stripMetadata).getAsJsonObject();

        metadata = String.format("{\"ANO_LECTIVO\":\"%s\", \"START_DATE\":\"%s\", \"END_DATE\":\"%s\"}",
                metadataJson.get("ANO_LECTIVO").getAsString(), "2021-01-04",
                metadataJson.get("END_DATE").getAsString());

        workingDocument.addProperty("metadata", metadata);
        sr.setRequest(json.toString());
    }
}
