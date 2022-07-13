package pt.ist.fenix.webapp.task.connect;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import pt.ist.fenixframework.FenixFramework;

public class HackAccounts extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final User user1 = User.findByUsername("ist30047");
        final User user2 = User.findByUsername("ist419033");

//        final Account account1 = FenixFramework.getDomainObject("1697512810064735");
//        account1.getIdentity().setUser(User.findByUsername("ist419033"));

        Identity.USER_SWITCH_HANDLER.forEach(consumer -> consumer.accept(user2, user1));
    }

}