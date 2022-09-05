package pt.ist.fenix.webapp.task.connect;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Account;
import pt.ist.fenixframework.FenixFramework;

public class DeleteAccount extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Account account = FenixFramework.getDomainObject("571612902463223");
        account.delete();
    }

}