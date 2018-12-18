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

    private static final String[] FLUNKED_STUDENTS = new String[]{ "37090","52997","63108","65272","65504","65883","67253","68130","69389","69613","70939","73358","73708","73834","74123",
    		"74139","74243","76163","76345","76707","77076","78001","78067","78127","78343","78411","78694","78753","78991","79666","80900","80987","81211","81312","81640","81997","82013",
    		"82244","82541","83983","84770","84777","84974","85115","85338","86357" };
    static int count = 0;

    @Override
    public void runTask() throws Exception {
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

            final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear().getNextExecutionYear();

            LocalDate date = new LocalDate();
            if (!executionYear.containsDate(date)) {
                date = executionYear.getBeginDateYearMonthDay().toLocalDate();
            }
            RegistrationState.createRegistrationState(registration, null, date.toDateTimeAtStartOfDay(),
                    RegistrationStateType.FLUNKED);
        }
    }
}