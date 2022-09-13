package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class SetAnnulmentIntegrated extends CustomTask {

    @Override
    public void runTask() throws Exception {
//        fix("571557067892220");
//        fix("571557067892221");
//        fix("571557067892222");
        fix("1415981998014487");
    }

    private void fix(final String eventID) {
        Event event = FenixFramework.getDomainObject(eventID);
        event.getSapRequestSet().stream()
                .filter(sr -> sr.getIgnore())
                .forEach(sr -> {
                    sr.setSent(true);
                    sr.setIntegrated(true);
                    if (sr.getOriginalRequest() != null) {
                        sr.getOriginalRequest().setIgnore(true);
                    }
                });
    }
}
