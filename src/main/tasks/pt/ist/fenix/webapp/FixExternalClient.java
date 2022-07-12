package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixframework.FenixFramework;

public class FixExternalClient extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final SapRequest sapRequest = FenixFramework.getDomainObject("852658382927266");
        final JsonObject requestAsJson = sapRequest.getRequestAsJson();
        final JsonObject clientData = requestAsJson.get("clientData").getAsJsonObject();
        clientData.addProperty("companyName", "IST-ID");
        clientData.addProperty("street", "Av. Rovisco Pais");
        clientData.addProperty("postalCode", "1049-001");
        clientData.addProperty("nationality", "PT");
        clientData.addProperty("accountId", "ADHOC");
        sapRequest.setRequest(requestAsJson.toString());
    }

}