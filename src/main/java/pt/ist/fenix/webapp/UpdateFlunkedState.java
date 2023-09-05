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

    private static final String[] FLUNKED_STUDENTS = new String[]{"67861", "68354", "74265", "76163", "76537", "76782", "77076", "78072", "78343", "79838",
            "80939", "81060", "81183", "81400", "81495", "81884", "82425", "82605", "83659", "83953", "85326", "86289", "86422", "86844", "87238", "87676", "87685",
            "88020", "89192", "90079", "90952", "90953", "91162", "91188", "94243", "94325"};
    static int count = 0;
    private static ExecutionYear executionYear = null;

    @Override
    public void runTask() throws Exception {
        executionYear = ExecutionYear.readExecutionYearByName("2023/2024");
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

//        final List<Registration> transitionRegistrations = student.getTransitionRegistrations();
//        if (!transitionRegistrations.isEmpty()) {
//            for (Registration registration : transitionRegistrations) {
//                deleteRegistration(registration);
//            }
//        }

        final Set<Registration> activeRegistrations = getActiveRegistrations(student);
        if (activeRegistrations.size() != 1) {
            taskLog("Student: " + student.getNumber() + " has more than one active registration in degree admin office, it has "
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

    private void deleteRegistration(Registration registration) {
        taskLog("Delete Transitions Registration For " + registration.getDegree().getName());
        if (registration == null || !registration.isTransition()) {
            throw new RuntimeException("error.trying.to.delete.invalid.registration");
        }

        for (; registration.getStudentCurricularPlansSet().size() != 0; ) {
            final StudentCurricularPlan studentCurricularPlan = registration.getStudentCurricularPlansSet().iterator().next();
            if (!studentCurricularPlan.isBolonhaDegree()) {
                throw new RuntimeException("What?");
            }

            deleteCurriculumModules(studentCurricularPlan.getRoot());
            removeEmptyGroups(studentCurricularPlan.getRoot());

            final ExtraCurriculumGroup extraCurriculumGroup = studentCurricularPlan.getExtraCurriculumGroup();
            if (extraCurriculumGroup != null) {
                extraCurriculumGroup.deleteRecursive();
            }
            if (studentCurricularPlan.getRoot() != null) {
                studentCurricularPlan.getRoot().delete();
            }
            studentCurricularPlan.delete();
        }

        registration.delete();
    }

    protected void deleteCurriculumModules(final CurriculumModule curriculumModule) {
        if (curriculumModule == null) {
            return;
        }

        if (!curriculumModule.isLeaf()) {
            final CurriculumGroup curriculumGroup = (CurriculumGroup) curriculumModule;
            for (final CurriculumModule each : curriculumGroup.getCurriculumModulesSet()) {
                deleteCurriculumModules(each);
            }
        } else if (curriculumModule.isDismissal()) {
            curriculumModule.delete();
        } else {
            throw new RuntimeException("error.in.transition.state.can.only.remove.groups.and.dismissals");
        }
    }

    protected void removeEmptyGroups(final CurriculumGroup curriculumGroup) {
        if (curriculumGroup == null) {
            return;
        }

        for (final CurriculumModule curriculumModule : curriculumGroup.getCurriculumModulesSet()) {
            if (!curriculumModule.isLeaf()) {
                removeEmptyChildGroups((CurriculumGroup) curriculumModule);
            }
        }
    }

    private void removeEmptyChildGroups(final CurriculumGroup curriculumGroup) {
        for (final CurriculumModule curriculumModule : curriculumGroup.getCurriculumModulesSet()) {
            if (!curriculumModule.isLeaf()) {
                removeEmptyChildGroups((CurriculumGroup) curriculumModule);
            }
        }

        if (curriculumGroup.getCurriculumModulesSet().size() == 0 && !curriculumGroup.isRoot()
                && !curriculumGroup.isExtraCurriculum()) {
            curriculumGroup.deleteRecursive();
        }
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