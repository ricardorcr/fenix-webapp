package pt.ist.fenix.webapp;

import com.google.gson.JsonElement;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

public class CheckDuplicateFinalCreditPayment extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.CLOSE_INVOICE)
                .filter(this::hasDuplicate)
                .forEach(sr -> taskLog(sr.getEvent().getExternalId()));
    }

    private boolean hasDuplicate(final SapRequest closeInvoice) {
        final String invoiceNumber = getInvoiceNumber(closeInvoice);
        long count = closeInvoice.getEvent().getSapRequestSet().stream()
                .filter(sr -> sr != closeInvoice)
                .filter(sr -> sr.getRequestType() == SapRequestType.CLOSE_INVOICE)
                .filter(sr -> getInvoiceNumber(sr).equals(invoiceNumber))
                .count();
        if (count > 1) {
            taskLog("hummm %s %s%n", closeInvoice.getDocumentNumber(), closeInvoice.getEvent().getExternalId());
        }
        return count > 0;
    }

    private String getInvoiceNumber(final SapRequest request) {
        final JsonElement paymentDocument = request.getRequestAsJson().get("paymentDocument");
        return paymentDocument.getAsJsonObject().get("workingDocumentNumber").getAsString();
    }
}
