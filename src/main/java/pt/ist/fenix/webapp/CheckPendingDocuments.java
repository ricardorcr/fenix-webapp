package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRoot;

public class CheckPendingDocuments extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SapRoot.getInstance().getSapRequestSet().stream()
                .filter(sr -> !sr.getSent())
                .filter(sr -> !sr.getIntegrated())
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> sr.getDocumentDate().getYear() < 2024)
                .forEach(sr -> taskLog("Must be checked: %s\t%s%n", sr.getDocumentNumber(), sr.getEvent().getExternalId()));
    }
}
