package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.OccupationPeriodType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

public class DebugOccupationPeriods extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getExecutionYearsSet().stream()
                .flatMap(executionYear -> executionYear.getExecutionDegreesSet().stream())
                .flatMap(executionDegree -> executionDegree.getOccupationPeriodReferencesSet().stream())
                .filter(ref -> ref.getPeriodType() == OccupationPeriodType.LESSONS)
                //.filter(ref -> ref.getSemester().intValue() == executionSemester.getSemester().intValue())
                //.filter(ref -> overlap(ref.getCurricularYears().getYears(), curricularYears))
                .filter(ref -> ref.getCurricularYears() == null)
                .forEach(ref -> {
                    taskLog("ref has null years " + ref.getExternalId());
                });
    }
}