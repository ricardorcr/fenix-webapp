package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.util.Comparator;
import java.util.stream.Collectors;

public class CheckShiftsDistribution extends CustomTask {

    @Override
    public void runTask() throws Exception {
        ExecutionYear.readCurrentExecutionYear().getShiftDistribution().getShiftDistributionEntriesSet().stream()
                .filter(distribution -> distribution.getAbstractStudentNumber() >= 16000) //for 2nd phase
                .map(distribution -> distribution.getExecutionDegree())
                .collect(Collectors.toSet()).stream()
                .sorted(Comparator.comparing(ed -> ed.getDegreeCurricularPlan().getName()))
                .forEach(ed -> taskLog("%s%n", ed.getDegreeCurricularPlan().getName()));
    }
}
