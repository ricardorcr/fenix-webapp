package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.EventProcessor;
import pt.ist.fenixedu.giaf.invoices.SapEvent;

public class CheckPaymentYears extends CustomTask {

    @Override
    public void runTask() throws Exception {

        Bennu.getInstance().getSapRoot().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT || sr.getRequestType() == SapRequestType.ADVANCEMENT
                        || sr.getRequestType() == SapRequestType.PAYMENT_INTEREST)
                .filter(sr -> getPaymentDate(sr).contains("2018"))
                .filter(sr -> sr.getSapDocumentNumber() == null || sr.getSapDocumentNumber().contains("2019"))
                .filter(sr -> sr.getWhenCreated().getYear() == 2019 && sr.getWhenCreated().getMonthOfYear() >= 2)
                .filter(sr -> sr.getPayment() != null && sr.getPayment().getWhenRegistered().getYear() == 2018)
//                .forEach(sr ->
//                        taskLog("https://fenix.tecnico.ulisboa.pt/accounting-management/%s/details\t" +
//                                        "Nº: %s\tDataPagamento: %s\tDataCriação: %s\tDataRequest: %s\tIntegrated: %s\t%s%n",
//                                sr.getEvent().getExternalId(), sr.getDocumentNumber(), sr.getPayment().getWhenRegistered().toString("dd/MM/yyyy HH:mm"),
//                                sr.getPayment().getWhenProcessed().toString("dd/MM/yyyy HH:mm"),
//                                sr.getWhenCreated().toString("dd/MM/yyyy HH:mm"), sr.getIntegrated(), sr.getExternalId()))
                .forEach(this::correctPayment);
    }

    private void correctPayment(final SapRequest sapRequest) {
        SapEvent sapEvent = new SapEvent(sapRequest.getEvent());
        sapEvent.cancelDocument(sapRequest);
        EventProcessor.sync(() -> sapRequest.getEvent()); //sends cancelation
        EventProcessor.calculate(() -> sapRequest.getEvent()); //generates new payment
        EventProcessor.sync(() -> sapRequest.getEvent()); //sends new payment
    }

    private String getPaymentDate(final SapRequest sr) {
        final JsonObject request = sr.getRequestAsJson();
        final JsonObject workingDocument = request.getAsJsonObject("paymentDocument");
        return workingDocument.get("paymentDate").getAsString();
    }
}
