package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.FenixFramework;

public class UpdateAdvancementUsePaymentsJson extends CustomTask {

    final Integer[] number = new Integer[1];

//    @Override
//    public TxMode getTxMode() {
//        return TxMode.READ;
//    }

    @Override
    public void runTask() throws Exception {
        number[0] = 0;
        SapRoot.getInstance().getSapRequestSet().stream().filter(sr -> !sr.isInitialization())
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.PAYMENT_INTEREST)
                .filter(sr -> sr.getAdvancementRequest() != null /*&& sr.getRefund() != null*/)
                .filter(sr -> sr.getAdvancementRequest() == null || sr.getValue().equals(sr.getAdvancementRequest().getAdvancement()))
                .forEach(this::update);

        taskLog("Alterados %s requests.", number[0]);
    }

    private void update(SapRequest sapRequest) {
        if (!sapRequest.getIntegrated()) {
            try {
                FenixFramework.atomic(() -> {
                    JsonObject requestAsJson = sapRequest.getRequestAsJson();
                    JsonObject paymentDocument = requestAsJson.get("paymentDocument").getAsJsonObject();
                    //se não tiver quer dizer que já foi alterado
                    if (paymentDocument.has("documents")) {                        
                        paymentDocument.remove("documents");
                        paymentDocument.addProperty("isAdvancedPayment", true);
                        paymentDocument.addProperty("originatingOnDocumentNumber", getNADocumentNumber(sapRequest));
                        sapRequest.setRequest(requestAsJson.toString());
                        number[0] += 1;
                        taskLog("Alterei o request: %s\tdo evento: %s\tcriação request: %s%n", sapRequest.getDocumentNumber(), sapRequest.getEvent().getExternalId(), sapRequest.getWhenCreated());
                    }
                });
            } catch (Exception e) {
                taskLog("Problems with: %s %s %s%n", sapRequest.getDocumentNumber(), sapRequest.getEvent().getExternalId(), e.getMessage());
            }
        }
    }

    private String getNADocumentNumber(SapRequest sapRequest) {
        if (sapRequest.getAdvancementRequest() != null) {
            return sapRequest.getAdvancementRequest().getDocumentNumberForType("NA");
        } else {
            JsonObject requestAsJson = sapRequest.getRequestAsJson();
            JsonObject paymentDocument = requestAsJson.get("paymentDocument").getAsJsonObject();
            JsonArray documents = paymentDocument.get("documents").getAsJsonArray();
            return documents.get(0).getAsJsonObject().get("originDocNumber").getAsString();
        }
    }
}