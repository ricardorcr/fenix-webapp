package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.util.List;
import java.util.stream.Collectors;

public class CorrectAdvancementUseClientInfo extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getDocumentNumber().equals("NP445709"))
                .filter(sr -> !sr.getIntegrated())
                .filter(sr -> !sr.getSent())
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT)
                .filter(sr -> sr.getDocumentDate().getYear() == 2020)
                .forEach(this::process);
    }

    private void process(final SapRequest sapRequest) {
        sapRequest.getClientId();
        List<SapRequest> invoiceList = sapRequest.getEvent().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE
                        || sr.getRequestType() == SapRequestType.INVOICE_INTEREST)
                .filter(sr -> sapRequest.refersToDocument(sr.getDocumentNumber()))
                .filter(sr -> !sr.getClientId().equals(sapRequest.getClientId()))
                .collect(Collectors.toList());
        if (invoiceList.size() > 1) {
            taskLog("O request: %s (evento: %s) aponta para mais que uma factura?!%n",
                    sapRequest.getDocumentNumber(), sapRequest.getEvent().getExternalId());
        } else if (invoiceList.size() == 1) {
            SapRequest invoice = invoiceList.get(0);
            taskLog("Vou mudar o request %s do evento %s, de %s para %s%n",
                    sapRequest.getDocumentNumber(), sapRequest.getEvent().getExternalId(), sapRequest.getClientId(), invoice.getClientId());
            sapRequest.setClientId(invoice.getClientId());
            JsonObject requestAsJson = sapRequest.getRequestAsJson();
            requestAsJson.add("clientData", invoice.getClientJson());
            sapRequest.setRequest(requestAsJson.toString());
        }
    }
}
