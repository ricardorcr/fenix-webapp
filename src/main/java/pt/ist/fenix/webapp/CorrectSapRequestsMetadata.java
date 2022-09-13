package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;

public class CorrectSapRequestsMetadata extends CustomTask {

    @Override
    public void runTask() throws Exception {

        Arrays.asList("1125925676656459","","","","").stream()
                .map(oid -> (Event) FenixFramework.getDomainObject(oid))
                .flatMap(event -> event.getSapRequestSet().stream())
                .filter(sr -> sr.getRequestType() == SapRequestType.DEBT_CREDIT)
                .forEach(this::fix);
    }

    private void fix(final SapRequest sr) {
        JsonObject json = new JsonParser().parse(sr.getRequest()).getAsJsonObject();
        JsonObject workingDocument = json.get("workingDocument").getAsJsonObject();

        String metadata = workingDocument.get("metadata").getAsString();
        String stripMetadata = metadata.replace("\\", "");
        JsonObject metadataJson = new JsonParser().parse(stripMetadata).getAsJsonObject();

        taskLog("Fixed SapRequest: %s\tEvent: %s\t%s%n", sr.getDocumentNumber(), sr.getEvent().getExternalId(), metadataJson.get("Despacho").getAsString());

        metadata = String.format("{\"ANO_LECTIVO\":\"%s\", \"START_DATE\":\"%s\", \"END_DATE\":\"%s\", \"Despacho\":\"%s\"}",
                metadataJson.get("ANO_LECTIVO").getAsString(), metadataJson.get("START_DATE").getAsString(),
                metadataJson.get("END_DATE").getAsString(), "2022-07-01");

        workingDocument.addProperty("metadata", metadata);
        sr.setRequest(json.toString());
    }
}
