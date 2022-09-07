package pt.ist.fenix.webapp.task.academic.schedules;

import org.fenixedu.academic.domain.CourseLoad;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.Lesson;
import org.fenixedu.academic.domain.Shift;
import org.fenixedu.academic.domain.space.SpaceUtils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.spaces.domain.Space;

import pt.ist.fenixframework.FenixFramework;

public class MoveLessonInstances extends CustomTask {

    private Space tagusparkCampus = FenixFramework.getDomainObject("2448131360898");

    @Override
    public void runTask() throws Exception {
        ExecutionCourse executioncourseTagus = FenixFramework.getDomainObject("564560566181826");
        ExecutionCourse executioncourseAlameda = FenixFramework.getDomainObject("846035542882132");

        executioncourseTagus.getAssociatedShifts().forEach(shift -> {
            if (!isLessonFromTagus(shift)) {
                taskLog("\nMOVE %s, %s, %s - (%s)", shift.getExecutionCourse().getDegreePresentationString(),
                        shift.getExecutionCourse().getName(), shift.getPresentationName(), shift.getExternalId());
                shift.getCourseLoadsSet().forEach(cl -> {
                    CourseLoad courseLoad = getNewCourseLoad(executioncourseAlameda, cl);
                    shift.removeCourseLoads(cl);
                    shift.addCourseLoads(courseLoad);
                });
            }
        });

        taskLog("\nDone");
    }

    private CourseLoad getNewCourseLoad(ExecutionCourse executioncourseAlameda, CourseLoad cl) {
        CourseLoad result = executioncourseAlameda.getCourseLoadsSet().stream()
                .filter(eccl -> eccl.getType().equals(cl.getType())).findAny().orElse(null);

        if (result == null) {
            result = new CourseLoad(executioncourseAlameda, cl.getType(), cl.getUnitQuantity(), cl.getTotalQuantity());
        } else {
            if (!result.getTotalQuantity().equals(cl.getTotalQuantity())) {
                result.setTotalQuantity(cl.getTotalQuantity());
                result.setUnitQuantity(cl.getUnitQuantity());
            }
        }
        return result;
    }

    private boolean isLessonFromTagus(Shift shift) {
        return shift.getAssociatedLessonsSet().stream().allMatch(l -> isRoomFromTagus(l));
    }

    private boolean isRoomFromTagus(Lesson lesson) {
        if (lesson.getRoomOccupation() == null || lesson.getRoomOccupation().getSpace() == null) {
            return true;
        }
        return tagusparkCampus.equals(SpaceUtils.getSpaceCampus(lesson.getRoomOccupation().getSpace()));
    }

}