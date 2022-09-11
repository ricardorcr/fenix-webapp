package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class CheckCNAESCreatedAccounts extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess process = FenixFramework.getDomainObject("571432513831074");
/*
        process.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .map(application -> application.getAccount())
                //.filter(account -> !account.getEmail().startsWith("dges"))
                .filter(account -> account.getIdentity().getAccountSet().size() > 1)
                .forEach(account -> {
                    taskLog("%s : %s%n", account.getExternalId(), account.getEmail());
                    taskLog("   %s%n", account.getCreatedInstant().toString("yyyy-MM-dd HH:mm"));
                    account.getIdentity().getAccountSet().stream()
                            .filter(a -> a != account)
                            .forEach(a -> taskLog("   %s : %s%n",
                                    a.getCreatedInstant().toString("yyyy-MM-dd HH:mm"),
                                    a.getEmail()));
                });
 */
        process.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .map(application -> application.getAccount())
                .map(account -> account.getIdentity())
                .flatMap(identity -> identity.getAccountSet().stream())
                .filter(account -> account.getEmail().startsWith("deges"))
                .forEach(account -> {
                    taskLog("Hacking account creation date for %s%n", account.getEmail());
                    account.setCreatedInstant(account.getCreatedInstant().withYear(1972));
                });
    }

}