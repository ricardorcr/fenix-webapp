package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.ServiceAgreementTemplate;
import org.fenixedu.academic.domain.phd.debts.PhdRegistrationFeePR;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

public class CreatePhdRegistrationFeePR extends CustomTask {

    @Override
    public void runTask() throws Exception {
        ServiceAgreementTemplate sat = FenixFramework.getDomainObject("1975019236229123"); //Media Digitais
        new PhdRegistrationFeePR(new DateTime(), null, sat, new Money(30), Money.ZERO);
    }
}
