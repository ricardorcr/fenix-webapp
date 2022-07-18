package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Shift;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixedu.domain.SapRoot;
import pt.ist.fenixframework.FenixFramework;

public class TestShift extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Shift shift = FenixFramework.getDomainObject("565698732553987");   
        taskLog("#test: %s%n", shift != null);
    }

}