package pt.ist.fenix.webapp.task.accounting;

import com.google.gson.JsonObject;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.io.ByteArrayOutputStream;

public class CheckMbWayPayments extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("MbWay");
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.PAYMENT_INTEREST
                                || sr.getRequestType() == SapRequestType.ADVANCEMENT)
                .filter(sr -> {
                    final JsonObject requestAsJson = sr.getRequestAsJson();
                    final JsonObject paymentDocument = requestAsJson.get("paymentDocument").getAsJsonObject();
                    if (paymentDocument.has("sibsDate")) {
                        final String sibsDate = paymentDocument.get("sibsDate").getAsString();
                        if (sibsDate.equals("2023-04-06")) {
                            final String paymentMechanism = paymentDocument.get("paymentMechanism").getAsString();
                            return paymentMechanism.equals("MW");
                        }
                    }
                    return false;
                })
                .forEach(sr -> report(sr, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("pagamentos_mbway.xlsx", baos.toByteArray());
    }

    private void report(final SapRequest sr, final Spreadsheet spreadsheet) {
        final Spreadsheet.Row row = spreadsheet.addRow();
        row.setCell("RequestID", sr.getExternalId());
        row.setCell("EventID", sr.getEvent().getExternalId());
        row.setCell("SapRequest", sr.getDocumentNumber());
        row.setCell("Valor", sr.getValue().add(sr.getAdvancement()).getAmountAsString());
        row.setCell("TransactionID", getTransactionReference(sr));
    }

    public String getTransactionReference(final SapRequest sapRequest) {
        final String settlement = sapRequest.getPayment().getSibsPayment().getSettlement();
        if (settlement != null) {
            final String[] parts = settlement.split(",");
            return parts[48];
        }
        return null;
    }
}
