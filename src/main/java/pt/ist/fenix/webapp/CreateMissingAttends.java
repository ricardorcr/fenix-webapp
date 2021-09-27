package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Attends;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import java.util.Iterator;
import java.util.List;

public class CreateMissingAttends extends CustomTask {
    @Override
    public void runTask() throws Exception {
        final ExecutionSemester currentExecutionSemester = ExecutionSemester.readActualExecutionSemester();
        currentExecutionSemester.getEnrolmentsSet().stream()
                .filter(enrolment -> enrolment.getAttendsSet().isEmpty())
                .forEach(enrolment -> {
                    taskLog("Creating missing attends for: %s\t%s%n", enrolment.getRegistration().getNumber(), enrolment.getCurricularCourse().getName());
                    createAttend(enrolment, currentExecutionSemester);
                });
    }

    private void createAttend(final Enrolment enrolment, final ExecutionSemester executionSemester) {

        final Registration registration = enrolment.getRegistration();
        final List<ExecutionCourse> executionCourses = enrolment.getCurricularCourse().getExecutionCoursesByExecutionPeriod(executionSemester);
        taskLog("%s\t%s%n", enrolment.getCurricularCourse().getExternalId(), enrolment.getDegreeCurricularPlanOfDegreeModule().getName());

        ExecutionCourse executionCourse = null;
        if (executionCourses.size() > 1) {
            final Iterator<ExecutionCourse> iterator = executionCourses.iterator();
            while (iterator.hasNext()) {
                final ExecutionCourse each = iterator.next();
                executionCourse = each;
            }
        } else if (executionCourses.size() == 1) {
            executionCourse = executionCourses.iterator().next();
        }

        if (executionCourse != null) {
            final Attends attend = executionCourse.getAttendsByStudent(registration.getStudent());
            if (attend == null) {
                enrolment.addAttends(new Attends(registration, executionCourse));
            } else if (attend.getEnrolment() == null) {
                attend.setRegistration(registration);
                enrolment.addAttends(attend);
            } else {
                throw new DomainException("error.cannot.create.multiple.enrolments.for.student.in.execution.course",
                        executionCourse.getNome(), executionCourse.getExecutionPeriod().getQualifiedName());
            }
        }
    }
}
