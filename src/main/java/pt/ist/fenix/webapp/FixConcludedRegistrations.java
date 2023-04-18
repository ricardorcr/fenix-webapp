package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class FixConcludedRegistrations extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getRegistrationsSet().stream()
                .filter(r -> r.getDegree().isFirstCycle() && !r.getDegree().isSecondCycle())
                .filter(Registration::isActive)
                .filter(r -> {
                    final CycleCurriculumGroup firstCycle = r.getLastStudentCurricularPlan().getFirstCycle();
                    if (firstCycle != null) {
                        return firstCycle.isConcluded();
                    }
                    return false;
                })
                .filter(r -> r.getStudent().getRegistrationStream()
                            .filter(r2 -> r2 != r)
                            .filter(r2 -> r2.getDegree().isSecondCycle() && !r2.getDegree().isFirstCycle())
                            .anyMatch(r2 -> r.getLastStudentCurricularPlan().getFirstCycle().getCycleCourseGroup().getDestinationAffinitiesSet().stream()
                                                .map(DegreeModule::getDegree)
                                                .anyMatch(d -> d == r2.getDegree())))
                .forEach(r -> taskLog("%s\t%s%n", r.getPerson().getUsername(), r.getDegreeName()));
    }
}
