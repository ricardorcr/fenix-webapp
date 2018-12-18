package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import pt.ist.fenixframework.FenixFramework;

public class DeleteAccount extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Account account = FenixFramework.getDomainObject("1697512810074388");
        account.getApplicationSet().forEach(app -> app.delete());
        account.delete();

//        PersonalInformation pi = FenixFramework.getDomainObject("1697461270461529");
//        pi.delete();
    }
}
