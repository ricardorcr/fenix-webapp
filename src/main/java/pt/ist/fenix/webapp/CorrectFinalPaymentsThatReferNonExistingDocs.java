package pt.ist.fenix.webapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.util.Iterator;

public class CorrectFinalPaymentsThatReferNonExistingDocs extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.getIntegrated())
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT)
                .filter(this::isFinalPaymentAndNeedsFix)
                .forEach(sr -> taskLog("%s %s%n", sr.getEvent().getExternalId(), sr.getDocumentNumber()));
    }

    private boolean isFinalPaymentAndNeedsFix(final SapRequest payment) {
        JsonObject paymentDocument = payment.getRequestAsJson().get("paymentDocument").getAsJsonObject();
        JsonElement documents = paymentDocument.get("documents");
        if (documents != null) {
            JsonArray docs = documents.getAsJsonArray();
            Iterator<JsonElement> iterator = docs.iterator();
            while (iterator.hasNext()) {
                String originDocNumber = iterator.next().getAsJsonObject().get("originDocNumber").getAsString();
                if (originDocNumber.startsWith("NP")) {
                    if (doesNotExist(payment, originDocNumber)) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean doesNotExist(final SapRequest payment, final String originDocNumber) {
        return payment.getEvent().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                //.filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.CREDIT)
                .filter(sr -> hasDocNumber(sr, originDocNumber))
                .count() > 0;
    }

    private boolean hasDocNumber(final SapRequest sr, final String originDocNumber) {
        if (sr.getRequestType() == SapRequestType.PAYMENT) {
            return sr.getDocumentNumber().equals(originDocNumber);
        } else {
            JsonObject paymentDocument = sr.getRequestAsJson().get("paymentDocument").getAsJsonObject();
            return paymentDocument.get("paymentDocumentNumber").getAsString().equals(originDocNumber);
        }
    }
}
