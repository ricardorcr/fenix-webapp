package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.mobility.outbound.OutboundMobilityCandidacyPeriod;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class DeleteOutgoingPeriodCandidacy extends CustomTask {

    final private static String[] PERIOD_IDS = new String[] {"851245338198021"};

    @Override
    public void runTask() throws Exception {
        for (int iter = 0; iter < PERIOD_IDS.length; iter++) {
            OutboundMobilityCandidacyPeriod period = FenixFramework.getDomainObject(PERIOD_IDS[iter]);
            period.getOutboundMobilityCandidacyContestSet().forEach(c -> c.delete());
            period.delete();
        }
    }
}
