package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.ConnectSystem;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

/**
 * @author Tiago Pinho
 */
public class SetMobileWhenValidatedForConnectAccounts extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        ConnectSystem.getInstance().getAccountSet().stream()
                .filter(account -> account.getCreatedInstant() != null)
                .forEach(this::process);
    }

    private void process(final Account account) {
        final DateTime createdInstant = account.getCreatedInstant();
        FenixFramework.atomic(() -> {
            account.setMobileWhenValidated(createdInstant);
        });
    }
}