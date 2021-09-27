package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class CheckConcludedCycles extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Degree.readBolonhaDegrees().stream()
                .filter(d -> d.getDegreeType().isIntegratedMasterDegree())
                .flatMap(d -> d.getDegreeCurricularPlansSet().stream())
                .filter(dcp -> dcp.isActive())
                .flatMap(dcp -> dcp.getStudentCurricularPlansSet().stream())
                .filter(scp -> scp.isActive())
                .filter(scp -> scp.hasCycleCurriculumGroup(CycleType.SECOND_CYCLE) && !scp.hasConcludedCycle(CycleType.SECOND_CYCLE) && scp.getSecondCycle().getAprovedEctsCredits() > 0.0d)
                .filter(scp -> scp.hasCycleCurriculumGroup(CycleType.FIRST_CYCLE) && !scp.hasConcludedCycle(CycleType.FIRST_CYCLE) &&  scp.getFirstCycle().getAprovedEctsCredits() > 0.0d)
                .forEach(scp -> {
                    final Person person = scp.getRegistration().getPerson();
                    taskLog("%s\t%s\t%s%n", person.getUsername(), person.getName(), scp.getDegreeCurricularPlan().getName());
                });
    }
}