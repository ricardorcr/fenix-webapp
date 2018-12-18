package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixframework.FenixFramework;

public class AnnulSapDocument extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SapRequest requestToAnnul = FenixFramework.getDomainObject("1697083312570369");
        JsonObject jsonAnnulled = new JsonParser().parse(requestToAnnul.getRequest()).getAsJsonObject();
        JsonObject workDocument = jsonAnnulled.get("workingDocument").getAsJsonObject();
        workDocument.addProperty("workStatus", "A");
        workDocument.addProperty("documentDate", new DateTime().toString("yyyy-MM-dd HH:mm:ss"));

        SapRequest sapRequestAnnulled =
                new SapRequest(requestToAnnul.getEvent(), requestToAnnul.getClientId(), requestToAnnul.getValue(),
                requestToAnnul.getDocumentNumber(), requestToAnnul.getRequestType(), requestToAnnul.getAdvancement(),
                        jsonAnnulled);

        requestToAnnul.setAnulledRequest(sapRequestAnnulled);
        requestToAnnul.setIgnore(true);
        sapRequestAnnulled.setIgnore(true);
    }
}
