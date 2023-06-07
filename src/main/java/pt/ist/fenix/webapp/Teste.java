package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.ServiceAgreementTemplate;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityPR;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

public class CreatePhdGratuityPR extends CustomTask {

    @Override
    public void runTask() throws Exception {
        ServiceAgreementTemplate agreementTemplate = FenixFramework.getDomainObject("1975019236229123");
        PhdGratuityPR gratuityPR = new PhdGratuityPR(new DateTime(2022,9,1,0,0),
                null, agreementTemplate, new Money(2750), 0.01);
    }

}