package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.degreeStructure.StandaloneCourseGroup;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class ConnectStandaloneGroupsInSCPs extends CustomTask {

    @Override
    public void runTask() throws Exception {
        ExecutionYear.readCurrentExecutionYear().getRegistrationDataByExecutionYearSet().stream()
                .map(RegistrationDataByExecutionYear::getRegistration)
                .filter(r -> r.getDegree().getDegreeType().isStrictlyFirstCycle())
                .map(Registration::getActiveStudentCurricularPlan)
                .filter(scp -> scp != null && scp.getStandaloneCurriculumGroup() != null
                        && scp.getStandaloneCurriculumGroup().getDegreeModule() == null)
                .forEach(scp -> {
                    final StandaloneCourseGroup dcpStandaloneGroup = scp.getDegreeCurricularPlan().getStandaloneCourseGroup();
                    scp.getStandaloneCurriculumGroup().setDegreeModule(dcpStandaloneGroup);
                    taskLog("Student: %s\t%s now connected.%n", scp.getRegistration().getNumber(), scp.getName());
                });
    }
}
