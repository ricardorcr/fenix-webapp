package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;

public class Teste extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Arrays.asList("1134133360216070", "1134133360216071").stream()
                .map(oid -> (SapRequest) FenixFramework.getDomainObject(oid))
                .forEach(sr -> sr.setIntegrated(true));
    }
}
