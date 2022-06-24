package pt.ist.fenix.webapp.task.institutional.dataAuthorization;

import org.fenixedu.academic.domain.DomainOperationLog;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixedu.integration.domain.CardDataAuthorizationLog;

public class ShowDataAuthorizations extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final User user = User.findByUsername("ist423218");
        user.getPerson().getDomainOperationLogsSet().stream()
                .filter(CardDataAuthorizationLog.class::isInstance)
                .map(CardDataAuthorizationLog.class::cast)
                .sorted(DomainOperationLog.COMPARATOR_BY_WHEN_DATETIME)
                .forEach(log -> {
                    taskLog("%s : %s : %s%n",
                            log.getWhenDateTime().toString("yyyy-MM-dd HH:mm:ss"),
                            log.getTitle(),
                            log.getAnswer());
                });
    }

}