package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixedu.domain.SapRequestType;

public class CheckReimbursementDebt extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getAccountingEventsSet().stream()
                .filter(event -> !event.getSapRequestSet().isEmpty())
                .filter(event -> event.getSapRequestSet().stream().anyMatch(sr -> sr.getRequestType().equals(SapRequestType.DEBT)))
                .filter(event -> event.getSapRequestSet().stream().anyMatch(sr -> sr.getRequestType().equals(SapRequestType.REIMBURSEMENT)))
                .forEach(event -> {
                    event.getSapRequestSet().stream()
                            .filter(sr -> sr.getRequestType() == SapRequestType.REIMBURSEMENT)
                            .filter(sr -> !sr.isInitialization())
                            .filter(sr -> sr.getDocumentDate().getYear() == 2023)
                            .filter(sr -> sr.getEvent().getSapRequestSet().stream().noneMatch(sr1 -> sr1.getRequestType() == SapRequestType.DEBT_CREDIT && sr1.getValue().equals(sr.getValue())))
                            .forEach(sr -> taskLog("Evento: %s%n", sr.getEvent().getExternalId()));
                });
    }
}
