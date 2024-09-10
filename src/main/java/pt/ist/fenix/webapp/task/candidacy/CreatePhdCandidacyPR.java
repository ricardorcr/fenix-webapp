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

        final ServiceAgreementTemplate serviceAgreementTemplate = FenixFramework.getDomainObject("1691139077832714"); //DEAETPT2020
        new PhdProgramCandidacyPR(serviceAgreementTemplate, new DateTime(2023,9,1,0,0), null, new Money(100));
    }
}
