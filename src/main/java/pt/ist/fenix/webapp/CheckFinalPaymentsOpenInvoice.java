package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;

import java.util.Set;
import java.util.TreeSet;

public class CheckFinalPaymentsOpenInvoice extends CustomTask {

    @Override
    public void runTask() throws Exception {

        Bennu.getInstance().getAccountingEventsSet().stream()
                .filter(e -> e.getSapRequestSet().size() > 2)
                .forEach(e -> hasNAandLastNP(e));
    }

    private boolean hasNAandLastNP(Event event) {
        SapRequest lastPayment = null;
        SapRequest lastCredit = null;
        Set<SapRequest> orderedRequests = new TreeSet<>(SapRequest.COMPARATOR_BY_ORDER);
        orderedRequests.addAll(event.getSapRequestSet());
        for (SapRequest sr : orderedRequests) {
            if (!sr.isInitialization() && !sr.getIgnore()) {
                if (sr.getRequestType() == SapRequestType.CREDIT) {
                    lastCredit = sr;
                }
                if (sr.getRequestType() == SapRequestType.PAYMENT) {
                    lastPayment = sr;
                }
            }
        }
        if (lastCredit != null && lastPayment != null) {
            if (lastCredit.getClientData().getVatNumber().equals(lastPayment.getClientData().getVatNumber())) {
                taskLog("%s\t%s\t%s\t%s%n", event.getExternalId(), getND(lastCredit), lastCredit.getDocumentNumber(),
                        lastPayment.getDocumentNumber());
                return lastPayment.getOrder() > lastCredit.getOrder();
            }
        }
        return false;
    }

    private String getND(final SapRequest credit) {
        final JsonObject request = credit.getRequestAsJson();
        final JsonObject workingDocument = request.getAsJsonObject("workingDocument");
        return workingDocument.get("workOriginDocNumber").getAsString();
    }
}