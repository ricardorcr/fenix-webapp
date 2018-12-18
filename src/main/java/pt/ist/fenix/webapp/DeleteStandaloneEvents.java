package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.events.gratuity.StandaloneEnrolmentGratuityEvent;
import org.fenixedu.academic.domain.accounting.postingRules.gratuity.StandaloneEnrolmentGratuityPR;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DeleteStandaloneEvents extends CustomTask {

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        String[] ids = new String[]{"1130113269760024", "1130113269760014", "1693063223181355", "285688339628107", "285688339628062", "1130113269760028"};
        for (int iter = 0; iter < ids.length; iter++) {
            Event event = FenixFramework.getDomainObject(ids[iter]);
            if (event.is$$do$$Valid() && event instanceof StandaloneEnrolmentGratuityEvent && event.getAdjustedTransactions().isEmpty()
                    && event.getExemptionsSet().isEmpty()) {

                StandaloneEnrolmentGratuityEvent standaloneEvent = (StandaloneEnrolmentGratuityEvent) event;
                final List<StandaloneEnrolmentGratuityPR> toDelete = new ArrayList<StandaloneEnrolmentGratuityPR>();
                FenixFramework.atomic(() -> {
                    StandaloneEnrolmentGratuityPR standalonePR = new StandaloneEnrolmentGratuityPR(standaloneEvent.getStartDate(), null,
                            standaloneEvent.getStudentCurricularPlan().getDegreeCurricularPlan().getServiceAgreementTemplate(),
                            BigDecimal.valueOf(1000000000), BigDecimal.valueOf(0), BigDecimal.ZERO);
                    taskLog("Canceling event: %s%n", event.getExternalId());
                    event.forceCancel(User.findByUsername("ist24616").getPerson(), "Event is to be deleted");
                    toDelete.add(standalonePR);
                });

                FenixFramework.atomic(() -> {
                    taskLog("Going to delete event: %s - amount to pay: %s%n", event.getExternalId(), event.calculateAmountToPay(new DateTime()));
                    event.delete();
                    toDelete.get(0).delete();
                    taskLog("Deleted event %s%n", event.getExternalId());
                });
            }
        }
    }
}
