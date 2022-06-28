package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixframework.FenixFramework;

public class FixSapRequestExemptionConnection extends CustomTask {

    @Override
    public void runTask() throws Exception {

        final SapRequest sapRequest = FenixFramework.getDomainObject("852658382810560");
        sapRequest.setCreditId("571230650373239");
    }

}