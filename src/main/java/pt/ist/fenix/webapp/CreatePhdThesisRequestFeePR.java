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
        DateTime startDate = new DateTime(2023,9,1,0,0);
        Money amount = new Money(500);
        ServiceAgreementTemplate serviceAgreementTemplate = FenixFramework.getDomainObject("1975019236229123"); //Media Digitais
        new PhdThesisRequestFeePR(startDate, null, serviceAgreementTemplate, amount);
    }
}
