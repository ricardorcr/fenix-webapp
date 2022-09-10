package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.stream.Collectors;

public class DebugDgesImport extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        final Degree degree = FenixFramework.getDomainObject("2761663971476");
        for (final DegreeCurricularPlan degreeCurricularPlan : degree.getDegreeCurricularPlansSet()) {
            for (final ExecutionDegree executionDegree : degreeCurricularPlan.getExecutionDegreesSet()) {
                if (executionDegree != null && executionDegree.getCampus() != null) {
                    taskLog("%s : Campus: %s : %s%n",
                            executionDegree.getExecutionYear().getYear(),
                            executionDegree.getCampus().getExternalId(),
                            executionDegree.getCampus().getName());
                }
            }
        }
        taskLog("isTagus: %s%n", isTagus(degree));
    }

    private static boolean isTagus(final Degree degree) {
        return degree.getCurrentCampus().stream()
                .map(campus -> campus.getName())
                .collect(Collectors.joining(", "))
                .indexOf("Tagus") >= 0;
    }

}