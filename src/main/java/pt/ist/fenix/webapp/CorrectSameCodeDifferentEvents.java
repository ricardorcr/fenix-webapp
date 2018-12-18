package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventState;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.FenixFramework;

public class CorrectSameCodeDifferentEvents extends CustomTask {

    @Override
    public void runTask() throws Exception {

        //there is only one payment, just need to get a new code for the person that has not made a payment yet
//        Event eventToGetNewCode = FenixFramework.getDomainObject("1126058820641582"); //ist142099
//        eventToGetNewCode.getEventPaymentCodeEntrySet().forEach(ce ->
//                ce.setPaymentCode(Bennu.getInstance().getPaymentCodePool().getAvailablePaymentCode())
//        );
//
//        fix("564470371894766", "1129846981788838", true); //ist426066 e ist186500

        //will set the closed event with a new paymentCode so that there are no more duplicates
        Event eventToGetNewCode = FenixFramework.getDomainObject("1134124769215575"); //ist193385
        eventToGetNewCode.getEventPaymentCodeEntrySet().forEach(ce ->
                ce.setPaymentCode(Bennu.getInstance().getPaymentCodePool().getAvailablePaymentCode())
        );
        fix("564470371894016", "1134124769215575", true); //ist426514 e ist193385

        //will set the closed event with a new paymentCode so that there are no more duplicates
        Event eventToGetNewCode2 = FenixFramework.getDomainObject("1134150539019415"); //ist193362
        eventToGetNewCode2.getEventPaymentCodeEntrySet().forEach(ce ->
                ce.setPaymentCode(Bennu.getInstance().getPaymentCodePool().getAvailablePaymentCode())
        );

        deleteExemption("1134150539019415");
        fix("564470371894017", "1134150539019415", false); //ist193027 e ist193362
        fix("282995395178601", "1134150539019415", true); //ist193027 e ist193362
    }

    private void deleteExemption(final String eventId) {
        final Event event = FenixFramework.getDomainObject(eventId);
        event.getExemptionsSet().forEach(ex -> ex.delete());
    }

    private void fix(String transactionIdToMove, String correctEventId, boolean closeEvent) {
        final AccountingTransaction at = FenixFramework.getDomainObject(transactionIdToMove);
        final Event correctEvent = FenixFramework.getDomainObject(correctEventId);

        at.getSapRequestSet().forEach(sr -> new SapEvent(at.getEvent()).cancelDocument(sr));
        at.setEvent(correctEvent);
        correctEvent.getEventPaymentCodeEntrySet().forEach(ce ->
                ce.setPaymentCode(Bennu.getInstance().getPaymentCodePool().getAvailablePaymentCode())
        );
        if (closeEvent) {
            correctEvent.forceChangeState(EventState.CLOSED, new DateTime());
        }
    }
}
