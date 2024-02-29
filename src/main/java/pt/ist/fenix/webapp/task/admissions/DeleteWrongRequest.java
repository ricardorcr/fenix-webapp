package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.smartForms.domain.Request;
import pt.ist.fenixframework.FenixFramework;

public class DeleteWrongRequest extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Request request = FenixFramework.getDomainObject("2823816443068564");
        request.deleteUnchecked();
    }
}
