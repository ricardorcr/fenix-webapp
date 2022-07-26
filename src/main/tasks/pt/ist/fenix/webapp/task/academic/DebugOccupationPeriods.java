package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.CurricularYearList;
import org.fenixedu.academic.domain.OccupationPeriodType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;

import java.util.Collections;
import java.util.stream.Collectors;

public class DebugOccupationPeriods extends WriteCustomTask {
    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getExecutionYearsSet().stream()
                .flatMap(executionYear -> executionYear.getExecutionDegreesSet().stream())
                .flatMap(executionDegree -> executionDegree.getOccupationPeriodReferencesSet().stream())
                .filter(ref -> ref.getPeriodType() == OccupationPeriodType.LESSONS)
                //.filter(ref -> ref.getSemester().intValue() == executionSemester.getSemester().intValue())
                //.filter(ref -> overlap(ref.getCurricularYears().getYears(), curricularYears))
                .filter(ref -> ref.getCurricularYears() == null)
                .peek(ref -> {
                    ref.setCurricularYears(new CurricularYearList(Collections.emptyList()));
                })
                .map(ref -> ref.getExecutionDegree().getExecutionYear())
                .collect(Collectors.toSet())
                .forEach(executionYear -> taskLog("%s%n", executionYear.getName()));
/*
                .forEach(ref -> {
                    taskLog("ref has null years " + ref.getExternalId());
                });
 */
    }
}