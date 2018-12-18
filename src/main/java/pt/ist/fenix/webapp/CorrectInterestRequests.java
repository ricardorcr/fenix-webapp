package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

public class CorrectInterestRequests extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getRequestType() == SapRequestType.INVOICE_INTEREST || sr.getRequestType() == SapRequestType.PAYMENT_INTEREST)
                .filter(this::hasProblem)
                .forEach(sr -> taskLog("Doc %s duplicado, no evento: %s%n", sr.getDocumentNumber(), sr.getEvent().getExternalId()));
    }

    private boolean hasProblem(final SapRequest sapRequest) {
        return sapRequest.getEvent().getSapRequestSet().stream()
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr != sapRequest)
                .filter(sr -> sr.getRequestType() == sapRequest.getRequestType())
                .filter(sr -> sr.getValue().equals(sapRequest.getValue()))
                .anyMatch(sr -> sr.getPayment() == sapRequest.getPayment());
    }
}
