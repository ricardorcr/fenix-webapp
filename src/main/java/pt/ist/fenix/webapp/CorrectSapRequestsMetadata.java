package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixframework.FenixFramework;

public class CorrectSapRequestsMetadata extends CustomTask {

    @Override
    public void runTask() throws Exception {

        SapRequest sapRequest = FenixFramework.getDomainObject("1134133360154334");
        fix(sapRequest, "2021-07-20");
        sapRequest = FenixFramework.getDomainObject("852658382586443");
        fix(sapRequest, "2021-09-01");
        sapRequest = FenixFramework.getDomainObject("852658382586441");
        fix(sapRequest, "2021-09-01");
        sapRequest = FenixFramework.getDomainObject("852658382585736");
        fix(sapRequest, "2021-09-01");
    }

    private void fix(final SapRequest sr, final String newDispatchDate) {
        JsonObject json = new JsonParser().parse(sr.getRequest()).getAsJsonObject();
        JsonObject workingDocument = json.get("workingDocument").getAsJsonObject();

        String metadata = workingDocument.get("metadata").getAsString();
        String stripMetadata = metadata.replace("\\", "");
        JsonObject metadataJson = new JsonParser().parse(stripMetadata).getAsJsonObject();

        metadata = String.format("{\"ANO_LECTIVO\":\"%s\", \"START_DATE\":\"%s\", \"END_DATE\":\"%s\", \"Despacho\":\"%s\"}",
                metadataJson.get("ANO_LECTIVO").getAsString(), metadataJson.get("START_DATE").getAsString(),
                metadataJson.get("END_DATE").getAsString(), newDispatchDate);

        workingDocument.addProperty("metadata", metadata);
        sr.setRequest(json.toString());
    }
}
