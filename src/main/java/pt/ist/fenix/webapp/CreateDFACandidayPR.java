package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.ServiceAgreementTemplate;
import org.fenixedu.academic.domain.accounting.postingRules.dfa.DFACandidacyPR;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

public class CreateDFACandidayPR extends CustomTask {

    @Override
    public void runTask() throws Exception {
        ServiceAgreementTemplate dfaMTICAgreementTemplate = FenixFramework.getDomainObject("1691139077832712");
        new DFACandidacyPR(new DateTime(), null, dfaMTICAgreementTemplate, new Money(100), Money.ZERO);
    }
}
