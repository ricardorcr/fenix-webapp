package pt.ist.fenix.webapp;

import java.util.HashSet;
import java.util.Set;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationState;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.academic.domain.studentCurriculum.ExtraCurriculumGroup;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.LocalDate;

public class UpdateFlunkedState extends CustomTask {

    private static final String[] FLUNKED_STUDENTS = new String[]{"37090","44637","50368","51410","52997","55314","63544","63916","65652","67377","68252","74328",
            "78072","78781","79728","81211","81400","81941","86844","86930","87532","87725","88042","89312","89481","90473","90750","90920","90957",
            "92516","92758","92973","94133","96307","97469","98945","98961","99419","99845","100667","100750","101214","102360","102420","103130","103230","103448",
            "103628","103663","103707","103855","104007","104053","104087","104094","104096","104100","104107","104143","104144","104618"};
    static int count = 0;
    private static ExecutionYear executionYear = null;

    @Override
    public void runTask() throws Exception {
        executionYear = ExecutionYear.readExecutionYearByName("2024/2025");
        User user = User.findByUsername("ist24616");
        Authenticate.mock(user, "Script UpdateFlunkedState");

        for (int iter = 0; iter < FLUNKED_STUDENTS.length; iter++) {

            final Student student = Student.readStudentByNumber(Integer.valueOf(FLUNKED_STUDENTS[iter]));
            if (student == null) {
                taskLog("Can't find student -> " + FLUNKED_STUDENTS[iter]);
                continue;
            }

            processStudent(student);
        }
        taskLog("Modified: " + count);
    }

    private void processStudent(final Student student) {
        taskLog("Process Student -> " + student.getNumber());

        //TODO the registration may not be active it can be in abandon, or there could be more than 1 active
        // it is necessary to specify which registration it is to flunk
        final Set<Registration> activeRegistrations = getActiveRegistrations(student);
        if (activeRegistrations.size() != 1) {
            taskLog("Student: " + student.getNumber() + " has zero or more than one active registration, it has "
                    + activeRegistrations.size());
            throw new RuntimeException();
        }

        // the student may have enrolments but they can be NA, this list is given bye NEP so we should just set and that's it
        // if they made a mistake the student can complain and the state can be reverted
//        else {
//            if (!activeRegistrations.iterator().next().getEnrolments(ExecutionYear.readCurrentExecutionYear()).isEmpty()) {
//                taskLog("Student: " + student.getNumber() + " has already enrolments this year");
//                return;
//            }
//        }
        count++;
        changeToFlunkedState(activeRegistrations.iterator().next());

        taskLog("*************************************");
    }

    private Set<Registration> getActiveRegistrations(final Student student) {
        final Set<Registration> result = new HashSet<Registration>();
        for (final Registration registration : student.getRegistrationsSet()) {
            if (registration.isActive() && registration.isBolonha() && !registration.getDegreeType().isEmpty()) {
                result.add(registration);
            }
        }
        return result;
    }

    private void changeToFlunkedState(final Registration registration) {
        taskLog("Change to Flunk State Registration -> " + registration.getDegreeCurricularPlanName());

        if (registration.getActiveStateType() != RegistrationStateType.FLUNKED) {
            LocalDate date = new LocalDate();
            if (!executionYear.containsDate(date)) {
                date = executionYear.getBeginDateYearMonthDay().toLocalDate();
            }
            RegistrationState.createRegistrationState(registration, null, date.toDateTimeAtStartOfDay(),
                    RegistrationStateType.FLUNKED);
        }
    }
}