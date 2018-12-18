package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.FenixFramework;

public class CorrectAnnulledSapRequest extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SapRequest annulledRequest = FenixFramework.getDomainObject("1415608335864197"); //ND4485
        JsonObject jsonToSend = (JsonObject) new JsonParser().parse(annulledRequest.getRequest());

        JsonObject workingDocument = jsonToSend.get("workingDocument").getAsJsonObject();
        workingDocument.addProperty("entryDate", new DateTime().toString("yyyy-MM-dd hh:mm"));
        String documentNumber = "ND" + SapRoot.getInstance().getAndSetNextDocumentNumber();
        workingDocument.addProperty("workingDocumentNumber", documentNumber);

        SapRequest requestToSend = new SapRequest(annulledRequest.getEvent(), annulledRequest.getClientId(),
                annulledRequest.getValue(), documentNumber, annulledRequest.getRequestType(),
                annulledRequest.getAdvancement(), jsonToSend);
    }
}
