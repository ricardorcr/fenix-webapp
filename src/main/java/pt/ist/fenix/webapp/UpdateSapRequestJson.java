package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;

public class UpdateSapRequestJson extends CustomTask {

    @Override
    public void runTask() throws Exception {
//        final DateTime newDate = new DateTime(2024, 9, 01, 0, 0);
//        final String dateString = newDate.toString("yyyy-MM-dd HH:mm:ss");
//
//        Arrays.asList("NP1359225", "NP1359226", "NP1359227").stream()
//                .map(this::getSapRequest)
//                .forEach(sapRequest -> updateDate(sapRequest, dateString));

        Arrays.asList("571183405829810", "571183405829811").stream()
                .map(oid -> (SapRequest) FenixFramework.getDomainObject(oid))
                .forEach(sapRequest -> {
                    String request = sapRequest.getRequest();
                    request = request.replace("2025-01-08", "2024-12-31");
                    request = request.replace("2025-12-31", "2024-12-31");
                    sapRequest.setRequest(request);
                });
    }

    private void updateDate(final SapRequest sr, final String dateString) {
        final JsonObject requestAsJson = sr.getRequestAsJson();
        requestAsJson.addProperty("fromDate", dateString);
        requestAsJson.addProperty("toDate", dateString);

        final JsonObject paymentDocument = requestAsJson.get("paymentDocument").getAsJsonObject();
        paymentDocument.addProperty("paymentDate", dateString);
        sr.setRequest(requestAsJson.toString());
    }

    //
//    private void update(String naNumber, String npNumber) {
//        SapRequest creditNote = getSapRequest(naNumber);
//        SapRequest payment = getSapRequest(npNumber);
//        JsonObject requestAsJson = payment.getRequestAsJson();
//        JsonObject paymentDocument = requestAsJson.get("paymentDocument").getAsJsonObject();
//        paymentDocument.addProperty("paymentDate", creditNote.getDocumentDate().toString("yyyy-MM-dd HH:mm:ss"));
//        payment.setRequest(requestAsJson.toString());
//    }
//    
    private SapRequest getSapRequest(String documentNumber) {
        return SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getDocumentNumber().equals(documentNumber))
                .findAny().get();
    }
}