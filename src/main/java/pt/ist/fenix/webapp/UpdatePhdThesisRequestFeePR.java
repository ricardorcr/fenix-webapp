package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.postingRules.FixedAmountPR;
import org.fenixedu.academic.domain.phd.debts.PhdThesisRequestFeePR;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

public class UpdatePhdThesisRequestFeePR extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Money value = new Money(500);
        Bennu.getInstance().getPostingRulesSet().stream()
                .filter(PhdThesisRequestFeePR.class::isInstance)
                .filter(pr -> pr.isActive())
                .map(pr -> (PhdThesisRequestFeePR)pr)
                .forEach(pr -> pr.edit(value, null));

        //They are PhdThesisRequestFeePR but defined in the parent class
        final FixedAmountPR pr1 = FenixFramework.getDomainObject("614180349307");
        pr1.deactivate();
        new PhdThesisRequestFeePR(new DateTime().minus(1000), null, pr1.getServiceAgreementTemplate(), value);

        final FixedAmountPR pr2 = FenixFramework.getDomainObject("563564133744641");
        pr2.deactivate();
        new PhdThesisRequestFeePR(new DateTime().minus(1000), null, pr2.getServiceAgreementTemplate(), value);
    }
}
