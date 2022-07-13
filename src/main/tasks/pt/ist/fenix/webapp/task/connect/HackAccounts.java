package pt.ist.fenix.webapp.task.connect;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import pt.ist.fenixframework.FenixFramework;

public class HackAccounts extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Account account1 = FenixFramework.getDomainObject("853087879175300");
        account1.setEmail("ist419033@ulisboa.pt");
    }

}