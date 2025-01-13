package pt.ist.fenix.webapp.task.candidacy;

import org.fenixedu.academic.domain.accounting.ServiceAgreementTemplate;
import org.fenixedu.academic.domain.phd.candidacy.PhdProgramCandidacyPR;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

public class CreatePhdCandidacyPR extends CustomTask {

    @Override
    public void runTask() throws Exception {

        final ServiceAgreementTemplate serviceAgreementTemplate = FenixFramework.getDomainObject("2256494212939777"); //DETPT
        new PhdProgramCandidacyPR(serviceAgreementTemplate, new DateTime(2024,7,10,0,0), null, new Money(100));
    }
}
