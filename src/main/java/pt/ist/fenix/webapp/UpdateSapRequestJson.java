package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixframework.FenixFramework;

public class UpdateSapRequestJson extends CustomTask {

    @Override
    public void runTask() throws Exception {

        SapRequest sapRequest = FenixFramework.getDomainObject("852658382520005");
        String request = sapRequest.getRequest();
        request = request.replace("NA343439", "NA301202");
        request = request.replace("NP343440", "NP301203");
        sapRequest.setRequest(request);
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
//    }
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
//    private SapRequest getSapRequest(String documentNumber) {
//        return SapRoot.getInstance().getSapRequestSet().stream()
//            .filter(sr -> !sr.isInitialization())
//            .filter(sr -> !sr.getIgnore())
//            .filter(sr -> sr.getDocumentNumber().contentEquals(documentNumber))
//            .findAny().get();
    }
}