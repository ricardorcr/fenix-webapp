package pt.ist.fenix.webapp;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;

public class CorrectSapRequestsMetadata extends CustomTask {

    @Override
    public void runTask() throws Exception {

        Bennu.getInstance().getAccountingEventsSet().stream().filter(e -> !e.getSapRequestSet().isEmpty())
                .flatMap(e -> e.getSapRequestSet().stream()).filter(sr -> sr.getRequestType() == SapRequestType.DEBT)
                .filter(sr -> sr.getRequest().length() > 2).forEach(this::fix);

    }

    private void fix(SapRequest sr) {
        JsonObject json = new JsonParser().parse(sr.getRequest()).getAsJsonObject();
        JsonObject workingDocument = json.get("workingDocument").getAsJsonObject();

        String metadata = workingDocument.get("debtMetadata").getAsString();
        String stripMetadata = metadata.replace("\\", "");
        JsonObject metadataJson = new JsonParser().parse(stripMetadata).getAsJsonObject();

        metadata = String.format("{\"ANO_LECTIVO\":\"%s\", \"START_DATE\":\"%s\", \"END_DATE\":\"%s\"}",
                metadataJson.get("ANO_LECTIVO").getAsString(), metadataJson.get("START_DATE").getAsString(),
                metadataJson.get("END_DATE").getAsString());

        workingDocument.addProperty("debtMetadata", metadata);
        sr.setRequest(json.toString());
    }
}
