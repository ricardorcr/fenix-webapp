package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

public class DebugSiteCreation extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final Degree degree = Degree.readBySigla("Min-EG");
        taskLog("Degree = %s%n", degree);
    }
}