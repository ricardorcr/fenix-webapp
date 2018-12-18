package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.domain.SapRoot;

import java.util.HashMap;
import java.util.Map;

public class CheckDuplicateSapDocNumber extends CustomTask {

    @Override
    public void runTask() throws Exception {

        final Map<String, SapRequest> duplicates = new HashMap<>();
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> sr.getRequestType() != SapRequestType.DEBT && sr.getRequestType() != SapRequestType.DEBT_CREDIT)
                .filter(sr -> sr.getSapDocumentNumber() != null)
                .filter(sr -> sr.getOriginalRequest() == null)
                .forEach(sr -> {
                    SapRequest previous = duplicates.putIfAbsent(sr.getSapDocumentNumber(), sr);
                    if (previous != null) {
                        taskLog("O nº SAP: %s está repetido %s %s - Evento: %s%n",
                                sr.getSapDocumentNumber(), sr.getDocumentNumber(), sr.getDocumentDate().getYear(), sr.getEvent().getExternalId());
                    }
                });
    }
}
