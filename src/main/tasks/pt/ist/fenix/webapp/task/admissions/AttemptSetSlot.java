package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.service.RegistrationService;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class AttemptSetSlot extends WriteCustomTask {
    @Override
    public void runTask() throws Exception {
        final Application application = FenixFramework.getDomainObject("852890310675894");
        RegistrationService.addToConfirmationQueueIfNeeded(application);
    }
}