package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixedu.domain.SapRoot;

public class UpdatePaymentsOffsetSAP extends CustomTask {

    @Override
    public void runTask() throws Exception {
        SapRoot.getInstance().setOffsetDays(7);
    }
}
