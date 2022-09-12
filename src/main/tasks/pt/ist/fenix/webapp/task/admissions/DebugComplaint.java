package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.ApplicationComplaint;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.connect.domain.Account;
import org.fenixedu.connect.domain.ConnectSystem;
import pt.ist.fenixframework.FenixFramework;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DebugComplaint extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Account account = User.findByUsername("ist24439").getIdentity().getAccountSet().stream().findAny().orElse(null);
        final AdmissionProcess admissionProcess = FenixFramework.getDomainObject("852907490541641");
        final Stream<ApplicationComplaint> complaints = admissionProcess.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(application -> application.isAllowedToView(account))
                //.filter(application -> searchStrings == null || application.matches(searchStrings))
                .flatMap(application -> application.getComplaintSet().stream())
                //.filter(complaint -> open == null || complaint.isOpen() == open)
        ;
        final Map<Account, List<ApplicationComplaint>> groupedComplaints = complaints
                .collect(Collectors.groupingBy(complaint -> ConnectSystem.getMostRelevantAccount(complaint.getApplication().getAccount())));
        taskLog("%s%n", groupedComplaints.size());

    }

}