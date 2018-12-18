package pt.ist.fenix.webapp;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class FixIgnoreAnnulledRequests extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getSapRoot().getSapRequestSet().stream()
                .filter(sr -> !sr.isInitialization())
                .filter(sr -> !sr.getIgnore())
                .filter(sr -> sr.getIntegrated())
                .filter(sr -> sr.getOriginalRequest() != null)
                .forEach(sr -> {
                    if (sr.getWhenCreated().getDayOfMonth() == 15
                            && sr.getWhenCreated().getMonthOfYear() == 11
                            && sr.getWhenCreated().getYear() == 2019) {
                        sr.setIgnore(true);
                    } else {
                        taskLog("Isto não devia acontecer %s %s %s%n", sr.getDocumentNumber(), sr.getWhenCreated(), sr.getEvent().getExternalId());
                    }
                });
    }
}
