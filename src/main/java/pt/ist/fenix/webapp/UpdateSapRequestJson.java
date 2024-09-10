package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.List;

public class UpdateSapRequestJson extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final DateTime newDate = new DateTime(2024, 07, 01, 0, 0);
        final String dateString = newDate.toString("yyyy-MM-dd HH:mm:ss");

        Arrays.asList("NP1200402", "NP1210316").stream()
                .map(this::getSapRequest)
                .forEach(sapRequest -> updateDate(sapRequest, dateString));

//        SapRequest sapRequest = FenixFramework.getDomainObject("3104458196130064");
//        String request = sapRequest.getRequest();
//        request = request.replace("2024-01-05", "2023-12-31");
//        sapRequest.setRequest(request);

//        
//        Map<String, String> pairs = new HashMap<String, String>();
////        pairs.put("NA431412", "NP431414");
////        pairs.put("NA430792", "NP430794");
////        pairs.put("NA429878", "NP429880");
////        pairs.put("NA429807", "NP429809");
////        pairs.put("NA429471", "NP429473");
////        pairs.put("NA423287", "NP423289");
//        pairs.put("NA429389", "NP429391");
//        
//        pairs.forEach((k,v) -> update(k,v));
//        
        //NP382831 faltou incluir o valor da nota de crédito neste pagamento final quando foi gerado
//        SapRequest finalPayment = FenixFramework.getDomainObject("1415608335927774");
//        JsonObject requestAsJson = finalPayment.getRequestAsJson();
//        JsonObject paymentDocument = requestAsJson.get("paymentDocument").getAsJsonObject();
//        JsonArray documents = paymentDocument.get("documents").getAsJsonArray();
//        JsonObject creditNote = new JsonObject();
//        creditNote.addProperty("amount", "3500.00");
//        creditNote.addProperty("isToDebit", false);
//        creditNote.addProperty("originDocNumber", "NP138423");
//        documents.add(creditNote);
//        finalPayment.setRequest(requestAsJson.toString());
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