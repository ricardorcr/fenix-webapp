package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class CheckCNAESCreatedAccounts extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess process = FenixFramework.getDomainObject("571432513831074");
        process.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .map(application -> application.getAccount())
                .filter(account -> !account.getEmail().startsWith("dges"))
                .forEach(account -> account.getEmail());
    }

}