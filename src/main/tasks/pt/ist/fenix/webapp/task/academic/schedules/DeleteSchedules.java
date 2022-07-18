package pt.ist.fenix.webapp.task.academic.schedules;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Lesson;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.spaces.domain.Space;

import pt.ist.fenixframework.FenixFramework;

public class DeleteSchedules extends CustomTask {
    @Override
    public void runTask() throws Exception {
        Space tagusparkCampus = FenixFramework.getDomainObject("2448131360898");

        ExecutionYear nextExecutionYear = ExecutionYear.readCurrentExecutionYear().getNextExecutionYear();
        ExecutionSemester executionSemester = nextExecutionYear.getFirstExecutionPeriod();

        taskLog("\nDelete all schedules from: %s  - EXCEPT TAGUS", executionSemester.getQualifiedName());

        Authenticate.mock(User.findByUsername("ist22986"), "Script");

        executionSemester.getAssociatedExecutionCoursesSet().forEach(executionCourse -> {
            executionCourse.getAssociatedShifts().forEach(shift -> {

                shift.getAssociatedLessonsSet().forEach(lesson -> {
                    if (!isRoomFromTagus(lesson, tagusparkCampus)) {
                        lesson.delete();

                    }
                });

                if (shift.getAssociatedLessonsSet().isEmpty()) {
                    shift.delete();
                }
            });
        });
        taskLog("\nDone!");
    }

    private boolean isRoomFromTagus(Lesson lesson, Space tagusparkCampus) {
        if (lesson.getRoomOccupation() == null || lesson.getRoomOccupation().getSpace() == null) {
            taskLog("\n\t\t-------- Empty room %s, %s, %s - (%s)",

                    lesson.getShift().getExecutionCourse().getDegreePresentationString(),
                    lesson.getShift().getExecutionCourse().getName(),

                    lesson.prettyPrint(), lesson.getExternalId());
            return true;
        }
        return lesson.getRoomOccupation().getSpace().equals(tagusparkCampus);
    }

}