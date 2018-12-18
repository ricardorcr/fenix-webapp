package pt.ist.fenix.webapp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.CompetenceCourse;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.organizationalStructure.CompetenceCourseGroupUnit;
import org.fenixedu.academic.domain.organizationalStructure.DepartmentUnit;
import org.fenixedu.academic.domain.organizationalStructure.ScientificAreaUnit;
import org.fenixedu.academic.domain.organizationalStructure.Unit;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import pt.ist.fenixedu.vigilancies.domain.VigilantGroup;
import pt.ist.fenixedu.vigilancies.domain.VigilantWrapper;
import pt.ist.fenixframework.FenixFramework;

public class RestoreViligantGroup extends CustomTask {

    @Override
    public void runTask() throws Exception {
        VigilantGroup alameda2Sem1920 = FenixFramework.getDomainObject("845253858820104");
        VigilantGroup alameda1Sem1920 = new VigilantGroup();
        alameda1Sem1920.setContactEmail(alameda2Sem1920.getContactEmail());
        alameda1Sem1920.setConvokeStrategy(alameda2Sem1920.getConvokeStrategy());
        alameda1Sem1920.setEmailSubjectPrefix(alameda2Sem1920.getEmailSubjectPrefix());
        alameda1Sem1920.setName("ALAMEDA 1º SEMESTRE");
        alameda1Sem1920.setPointsForConvoked(alameda2Sem1920.getPointsForConvoked());
        alameda1Sem1920.setPointsForDisconvoked(alameda2Sem1920.getPointsForDisconvoked());
        alameda1Sem1920.setPointsForDismissed(alameda2Sem1920.getPointsForDismissed());
        alameda1Sem1920.setPointsForDismissedTeacher(alameda2Sem1920.getPointsForDismissedTeacher());
        alameda1Sem1920.setPointsForMissing(alameda2Sem1920.getPointsForMissing());
        alameda1Sem1920.setPointsForMissingTeacher(alameda2Sem1920.getPointsForMissingTeacher());
        alameda1Sem1920.setPointsForTeacher(alameda2Sem1920.getPointsForTeacher());
        alameda1Sem1920.setRulesLink(alameda2Sem1920.getRulesLink());
        alameda1Sem1920.setExecutionYear(alameda2Sem1920.getExecutionYear());
        alameda1Sem1920.setUnit(alameda2Sem1920.getUnit());

        Set<VigilantWrapper> vigilantWrappers = Bennu.getInstance().getVigilantWrappersSet().stream()
                .filter(vw -> vw.getVigilantGroup() == null)
                .collect(Collectors.toSet());
        alameda1Sem1920.getExamCoordinatorsSet().addAll(alameda2Sem1920.getExamCoordinatorsSet());
        alameda1Sem1920.getVigilantWrappersSet().addAll(vigilantWrappers);
        List<ExecutionCourse> executionCourses = getBolonhaCourses(alameda2Sem1920.getUnit(), alameda2Sem1920.getExecutionYear().getFirstExecutionPeriod());
        alameda1Sem1920.getExecutionCoursesSet().addAll(executionCourses);
    }

    private List<ExecutionCourse> getBolonhaCourses(final Unit unit, final ExecutionSemester executionSemester) {

        List<CompetenceCourseGroupUnit> courseGroups = new ArrayList<CompetenceCourseGroupUnit>();
        List<ExecutionCourse> executionCourses = new ArrayList<ExecutionCourse>();
        if (unit.isDepartmentUnit()) {
            List<ScientificAreaUnit> scientificAreaUnits = ((DepartmentUnit) unit).getScientificAreaUnits();
            for (ScientificAreaUnit areaUnit : scientificAreaUnits) {
                courseGroups.addAll(areaUnit.getCompetenceCourseGroupUnits());
            }
        } else {
            courseGroups.addAll(((ScientificAreaUnit) unit).getCompetenceCourseGroupUnits());
        }

        for (CompetenceCourseGroupUnit courseGroup : courseGroups) {
            executionCourses.addAll(getBolonhaCoursesForGivenGroup(courseGroup, executionSemester));
        }

        return executionCourses;
    }

    private List<ExecutionCourse> getExecutionCoursesFromCompetenceCourses(final Collection<CompetenceCourse> competenceCourses, final ExecutionSemester executionSemester) {
        List<ExecutionCourse> courses = new ArrayList<ExecutionCourse>();
        for (CompetenceCourse course : competenceCourses) {
            courses.addAll(course.getExecutionCoursesByExecutionPeriod(executionSemester));
        }
        return courses;
    }
    
    private List<ExecutionCourse> getBolonhaCoursesForGivenGroup(final CompetenceCourseGroupUnit competenceCourseGroup, final ExecutionSemester executionSemester) {
        return getExecutionCoursesFromCompetenceCourses(competenceCourseGroup.getCompetenceCourses(), executionSemester);
    }
    
}
