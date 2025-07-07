package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.ServiceAgreementTemplate;
import org.fenixedu.academic.domain.phd.debts.PhdThesisRequestFeePR;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

public class CreatePhdThesisRequestFeePR extends CustomTask {

    @Override
    public void runTask() throws Exception {
        DateTime startDate = new DateTime(2025,1,18,0,0);
        Money amount = new Money(250);
        ServiceAgreementTemplate serviceAgreementTemplate = FenixFramework.getDomainObject("2256494212939777"); //Territorio
        new PhdThesisRequestFeePR(startDate, null, serviceAgreementTemplate, amount);
    }
}
