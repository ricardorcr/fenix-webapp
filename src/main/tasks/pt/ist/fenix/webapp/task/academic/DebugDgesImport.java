package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.stream.Collectors;

public class DebugDgesImport extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final Degree degree = FenixFramework.getDomainObject("2761663971476");
        taskLog("isTagus: %s%n", isTagus(degree));
    }

    private static boolean isTagus(final Degree degree) {
        return degree.getCurrentCampus().stream()
                .map(campus -> campus.getName())
                .collect(Collectors.joining(", "))
                .indexOf("Tagus") >= 0;
    }

}