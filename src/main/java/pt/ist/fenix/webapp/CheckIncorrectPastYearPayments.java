package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

public class CheckIncorrectPastYearPayments extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.PAYMENT)
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getRequestAsJson().get("productCode").getAsString().equals("0063"))
                .filter(this::isWrong)
                .forEach(sr -> taskLog("%s\t%s\t%s%n", sr.getDocumentNumber(), sr.getValue(), "https://fenix.tecnico.ulisboa.pt/sap-invoice-viewer/" + sr.getEvent().getExternalId()));
    }

    private boolean isWrong(final SapRequest sapRequest) {
        final String invoiceNumber = sapRequest.getDocumentNumberForType("ND");
        return !sapRequest.getEvent().getSapRequestSet().stream()
                .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE)
                .filter(sr -> sr.getDocumentNumber().equals(invoiceNumber))
                .filter(sr -> sr.getRequestAsJson().get("productCode").getAsString().equals("0063"))
                .findAny().isPresent();
    }
}
