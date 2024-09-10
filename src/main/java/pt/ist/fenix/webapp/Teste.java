package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class Teste extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final EventTemplate eventTemplate = FenixFramework.getDomainObject("290361264046082");
        eventTemplate.setCode(eventTemplate.getCode().trim());
    }
}
