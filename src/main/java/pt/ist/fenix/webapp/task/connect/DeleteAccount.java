package pt.ist.fenix.webapp.task.connect;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.connect.domain.Account;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.List;

public class DeleteAccount extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final List<String> accountIDs = Arrays.asList("1697512809802754", "1978987786035794", "1978987786035795", "1978987786035793",
                "1697512810033650", "1697512809742113", "1697512810009248");
        accountIDs.stream()
                .map(id -> (Account) FenixFramework.getDomainObject(id))
                .forEach(Account::delete);
    }
}
