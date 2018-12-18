package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.candidacy.IMDCandidacy;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixframework.FenixFramework;

public class DeleteSummaryFile extends CustomTask {

    @Override
    public void runTask() throws Exception {
        IMDCandidacy candidacy = FenixFramework.getDomainObject("1404454912169");
        candidacy.setSummaryFile(null);
    }
}
