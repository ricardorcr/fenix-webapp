package pt.ist.fenix.webapp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixframework.FenixFramework;

public class CorrectSapRequest extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SapRequest sapRequest = FenixFramework.getDomainObject("1978558289281668");
        sapRequest.setIntegrated(true);
        //changeDate(sapRequest);

        //Correct Credit requests
//        Bennu.getInstance().getSapRoot().getSapRequestSet().stream()
//                .filter(sr -> sr.getRequestType() == SapRequestType.CREDIT && !sr.getIntegrated())
//                .filter(sr -> needsNewDate(sr))
//                .forEach(sr -> changeDate(sr));
    }

    private void changeDate(final SapRequest sr) {
        final JsonObject request = sr.getRequestAsJson();
        final JsonObject workingDocument = request.getAsJsonObject("workingDocument");
        String documentDate = workingDocument.get("documentDate").getAsString();
        workingDocument.addProperty("documentDate", "2018-12-31 18:45:06");
//        final JsonObject paymentDocument = request.getAsJsonObject("paymentDocument");
        taskLog("%s - Previous date: %s - New Date: %s\n", sr.getExternalId(),
                documentDate, "2018-12-31 18:45:06");
//        paymentDocument.addProperty("paymentDate", documentDate);
        sr.setRequest(request.toString());
    }

    private boolean needsNewDate(final SapRequest sr) {
        final JsonObject request = sr.getRequestAsJson();
        final JsonObject workingDocument = request.getAsJsonObject("workingDocument");
        String documentDate = workingDocument.get("documentDate").getAsString();
        if (documentDate.contains("2018")) {
            final JsonObject paymentDocument = request.getAsJsonObject("paymentDocument");
            if (!paymentDocument.get("paymentDate").getAsString().equals(documentDate)) {
                return true;
            }
        }
        return false;
    }
}
