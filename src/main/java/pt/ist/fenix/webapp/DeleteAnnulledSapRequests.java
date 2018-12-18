package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class DeleteAnnulledSapRequests extends CustomTask {

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getAccountingEventsSet().stream().parallel().forEach(this::process);
    }

    private void process(Event event) {
        FenixFramework.atomic(() -> {
            event.getSapRequestSet().stream().filter(sr -> !sr.getIntegrated()).filter(sr -> sr.getOriginalRequest() != null)
                    .forEach(sr -> {
                        taskLog("Deleting annulation request %s - %s from event %s - %s\n", sr.getExternalId(),
                                sr.getDocumentNumber(),
                                sr.getEvent().getExternalId(), sr.getEvent().getPerson().getUsername());
                        sr.getEvent().getSapRequestSet().stream().filter(r -> !r.getIntegrated())
                                .filter(r -> r.getOriginalRequest() == null)
                                .filter(r -> r.getRequestType() == SapRequestType.INVOICE).forEach(r -> {
                                    taskLog("Deleting invoice request %s - %s from event %s - %s\n", r.getExternalId(),
                                            r.getDocumentNumber(), r.getEvent().getExternalId(),
                                            r.getEvent().getPerson().getUsername());
                                    r.delete();
                                });
                        sr.delete();
                    });
        });
    }
}
