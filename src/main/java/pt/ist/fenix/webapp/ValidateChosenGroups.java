package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicAccessRule;
import org.fenixedu.academic.domain.accessControl.academicAdministration.AcademicOperationType;
import org.fenixedu.academic.domain.curricularRules.executors.ruleExecutors.CurricularRuleLevel;
import org.fenixedu.academic.domain.degreeStructure.Context;
import org.fenixedu.academic.domain.degreeStructure.CourseGroup;
import org.fenixedu.academic.domain.degreeStructure.DegreeModule;
import org.fenixedu.academic.domain.enrolment.DegreeModuleToEnrol;
import org.fenixedu.academic.domain.enrolment.EnrolmentContext;
import org.fenixedu.academic.domain.enrolment.IDegreeModuleToEvaluate;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumGroup;
import org.fenixedu.academic.domain.studentCurriculum.StudentCurricularPlanEnrolment;
import org.fenixedu.academic.transitions.domain.DegreeModulePath;
import org.fenixedu.academic.transitions.domain.StudentChosenCourseGroups;
import org.fenixedu.academic.transitions.domain.StudentDegreeCurricularTransitionPlan;
import org.fenixedu.academic.transitions.service.TransitionService;
import org.fenixedu.academic.transitions.ui.AcademicTransitionsController;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.YearMonthDay;
import pt.ist.fenixframework.FenixFramework;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ValidateChosenGroups extends CustomTask {

    @Override
    public void runTask() throws Exception {
        ExecutionYear.readCurrentExecutionYear().getNextExecutionYear().getDegreeCurricularTransitionPlanFromDestinationSet().stream()
                .flatMap(transitionPlan -> transitionPlan.getStudentChosenCourseGroupsSet().stream())
                .forEach(this::validate);

//        Student.readStudentByNumber(90185).getStudentChosenCourseGroupsSet().forEach(chosenGroup -> validate(chosenGroup));
    }

    private void validate(StudentChosenCourseGroups studentChosenCourseGroups) {
        try {
            checkIfPossible(studentChosenCourseGroups);
        } catch (Exception e) {
            revert(studentChosenCourseGroups);
        }
    }

    private void revert(StudentChosenCourseGroups studentChosenCourseGroups) {
        final Person person = studentChosenCourseGroups.getStudent().getPerson();
        taskLog("%s\t%s\t%s%n", person.getUsername(), person.getName(),
                studentChosenCourseGroups.getTransitionPlan().getDestinationDegreeCurricularPlan().getName());
//        final Optional<StudentDegreeCurricularTransitionPlan> studentTransitionPlan = studentChosenCourseGroups.getStudent().getStudentDegreeCurricularTransitionPlanSet().stream()
//                .filter(studentPlan -> studentPlan.getDegreeCurricularTransitionPlan() == studentChosenCourseGroups.getTransitionPlan())
//                .findAny();
//        studentChosenCourseGroups.delete();
//        if (studentTransitionPlan.isPresent()) {
//            studentTransitionPlan.get().thaw();
//            if (studentTransitionPlan.get().getTransitionPlanRuleSet().stream().anyMatch(rule -> rule.getOrder() != null)) {
//                studentTransitionPlan.get().setApproved(false);
//            }
//        }
    }

    public void checkIfPossible(StudentChosenCourseGroups chosenGroups) {
        final DegreeCurricularPlan degreeCurricularPlan = chosenGroups.getTransitionPlan().getDestinationDegreeCurricularPlan();
        final Degree degree = chosenGroups.getTransitionPlan().getDestinationDegreeCurricularPlan().getDegree();
        final ExecutionYear executionYear = chosenGroups.getTransitionPlan().getDestinationExecutionYear();
        final ExecutionSemester executionSemester = executionYear.getExecutionSemesterFor(1);
        final Student student = chosenGroups.getStudent();
        final Set<DegreeModulePath> courseGroupPathsSet = chosenGroups.getCourseGroupPathsSet();

        final Boolean canCreate[] = new Boolean[] {false};
        final Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    FenixFramework.atomic(() -> {
                        final User user = student.getPerson().getUser();
                        Authenticate.mock(user, "StudentChosenCourseGroups");
                        final Registration registration = new Registration(student.getPerson(), student.getNumber(), degree);
                        final StudentCurricularPlan destinationStudentCurricularPlan = StudentCurricularPlan.createBolonhaStudentCurricularPlan(registration,
                                degreeCurricularPlan,
                                new YearMonthDay(),
                                executionSemester,
                                TransitionService.getCycleTypeFromDegreeCurricularPlan(degreeCurricularPlan));

                        final AcademicAccessRule rule = AcademicOperationType.STUDENT_ENROLMENTS.grant(user).get();
                        rule.changeProgramsAndOffices(
                                Collections.singleton(degree),
                                Collections.singleton(degree.getAdministrativeOffice()));

                        //Create current groups
                        final boolean oneCannotBeCreated = courseGroupPathsSet.stream().anyMatch(courseGroupPath -> !canCreateGroup(courseGroupPath, destinationStudentCurricularPlan, executionSemester));
                        if (!oneCannotBeCreated) {
                            canCreate[0] = true;
                        }
                        AcademicOperationType.STUDENT_ENROLMENTS.revoke(user);
                        throw new Error("Abort TX");
                    });
                } catch (final Throwable ex) {
                    if (!"Abort TX".equals(ex.getMessage())) {
                        ex.printStackTrace();
                    }
                } finally {
                    Authenticate.unmock();
                }
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new Error(e);
        }

        if (!canCreate[0]) {
            throw new DomainException(Optional.of(AcademicTransitionsController.BUNDLE), "error.student.choose.group.notPossible");
        }
    }

    private boolean canCreateGroup(final DegreeModulePath courseGroupPath, final StudentCurricularPlan destinationStudentCurricularPlan,
                                   final ExecutionSemester executionSemester) {
        if (courseGroupPath == null) {
            return true;
        }

        final CourseGroup courseGroup = courseGroupPath.getCourseGroup();
        final CurriculumGroup curriculumGroup = destinationStudentCurricularPlan.findCurriculumGroupFor(courseGroup);
        if (curriculumGroup == null) {
            final CourseGroup parent = courseGroupPath.getParentPath() == null
                    ? destinationStudentCurricularPlan.getRoot().getDegreeModule() : courseGroupPath.getParentPath().getCourseGroup();
            CurriculumGroup parentCurriculumGroup = destinationStudentCurricularPlan.findCurriculumGroupFor(parent);
            if (parentCurriculumGroup == null) {
                if (!canCreateGroup(courseGroupPath.getParentPath(), destinationStudentCurricularPlan, executionSemester)){
                    return false;
                }
                //the group may be already created by the parent
                if (destinationStudentCurricularPlan.findCurriculumGroupFor(courseGroup) != null) {
                    return true;
                }
                parentCurriculumGroup = destinationStudentCurricularPlan.findCurriculumGroupFor(parent);
            }

            final Context context = courseGroup.getParentContextsByExecutionSemester(executionSemester).iterator().next();
            DegreeModuleToEnrol moduleToEnrol = new DegreeModuleToEnrol(parentCurriculumGroup, context, executionSemester);

            Set<IDegreeModuleToEvaluate> degreeModulesToEnrol = new HashSet<>();
            degreeModulesToEnrol.add(moduleToEnrol);
            final EnrolmentContext enrolmentContext =
                    new EnrolmentContext(destinationStudentCurricularPlan, executionSemester, degreeModulesToEnrol, new ArrayList<>(),
                            CurricularRuleLevel.ENROLMENT_WITH_RULES);
            try {
                StudentCurricularPlanEnrolment.createManager(enrolmentContext).manage();
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }
}
