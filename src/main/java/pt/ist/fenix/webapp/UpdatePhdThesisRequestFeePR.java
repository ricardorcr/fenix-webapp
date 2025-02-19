package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.PostingRule;
import org.fenixedu.academic.domain.phd.debts.PhdThesisRequestFeePR;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class UpdatePhdThesisRequestFeePR extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Money value = new Money(250);
        Bennu.getInstance().getPostingRulesSet().stream()
                .filter(PhdThesisRequestFeePR.class::isInstance)
                .filter(PostingRule::isActive)
                .map(pr -> (PhdThesisRequestFeePR)pr)
                .forEach(pr -> pr.edit(value, null));
    }
}
