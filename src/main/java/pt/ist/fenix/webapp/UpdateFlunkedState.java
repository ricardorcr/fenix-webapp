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

    private static final String[] FLUNKED_STUDENTS = new String[]{ "21975","44598","52327","53311","56072","56586","58727","60109","63990","64663","64722","64728",
            "65311","65551","65951","65989","66438","66718","67581","68148","68205","69464","70010","70044","71051","72655","75424","76315","76536","77075","77918",
            "78211","78343","78411","79351","79516","79733","79740","81211","81640","81863","81884","82198","82265","82318","82425","82520","84045","84667","85215",
            "86371","86380","86444","86644","86874","86920","86930","86931","87552","88020","88215","88225","88643","90952","90953" };
    static int count = 0;
    private static ExecutionYear executionYear = null;

    @Override
    public void runTask() throws Exception {
        executionYear = ExecutionYear.readExecutionYearByName("2022/2023");

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