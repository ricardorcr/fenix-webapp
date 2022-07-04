package pt.ist.fenix.webapp.task.academic.schedules;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class DeleteSchedules extends CustomTask {
    @Override
    public void runTask() throws Exception {
        ExecutionYear nextExecutionYear = ExecutionYear.readCurrentExecutionYear().getNextExecutionYear();
        ExecutionSemester executionSemester = nextExecutionYear.getLastExecutionPeriod();

        taskLog("Delete all schedules from: %s", executionSemester.getQualifiedName());

        Authenticate.mock(User.findByUsername("ist22986"), "Script");

        executionSemester.getAssociatedExecutionCoursesSet().forEach(executionCourse -> {
            executionCourse.getSchoolClasses().forEach(schoolClass -> {
                taskLog("Delete " + schoolClass.getNome());
                schoolClass.delete();
            });
            executionCourse.getAssociatedShifts().forEach(shift -> {
                taskLog("Delete " + shift.getNome());
                shift.delete();
            });
        });
        taskLog("\nDone!");
    }

}