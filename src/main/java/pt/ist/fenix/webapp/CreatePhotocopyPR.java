package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.events.serviceRequests.PhotocopyRequestEvent;
import org.fenixedu.academic.domain.accounting.postingRules.serviceRequests.PhotocopyRequestPR;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

public class CreatePhotocopyPR extends CustomTask {

    @Override
    public void runTask() throws Exception {

        PhotocopyRequestEvent photocopyEvent = FenixFramework.getDomainObject("3564824320596");
        PhotocopyRequestPR photocopyRequestPR = new PhotocopyRequestPR(photocopyEvent.getAcademicServiceRequest().getAdministrativeOffice()
                .getServiceAgreementTemplate(), new DateTime(2013,9,01,0,0),
                null, new Money(5), new Money(0.5));
        taskLog(photocopyRequestPR.getExternalId());
    }
}
