package pt.ist.fenix.webapp.task.accounting;

import com.google.gson.JsonObject;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;

public class UpdateRequestClientData extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Arrays.asList("3385933172835943", "3385933172835944").forEach(requestID -> {
            final SapRequest sapRequest = FenixFramework.getDomainObject(requestID);
            final JsonObject requestAsJson = sapRequest.getRequestAsJson();
            final JsonObject clientData = requestAsJson.get("clientData").getAsJsonObject();
            clientData.addProperty("clientId", "PT252047273");
            clientData.addProperty("fiscalCountry", "PT");
            clientData.addProperty("country", "PT");
            clientData.addProperty("vatNumber", "PT252047273");
            clientData.addProperty("street", "Pracetas das Urmeiras lote 3 4ºD");
            clientData.addProperty("city", "Loures");
            clientData.addProperty("region", "Loures");
            clientData.addProperty("postalCode", "2670-534");
            sapRequest.setClientId("PT252047273");
            sapRequest.setRequest(requestAsJson.toString());
        });
    }
}
