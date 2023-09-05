package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.OccupationPeriod;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.YearMonthDay;
import pt.ist.fenixframework.FenixFramework;

public class Teste extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final ExecutionDegree executionDegree = FenixFramework.getDomainObject("1126758900301977");
        final ExecutionCourse executionCourse = FenixFramework.getDomainObject("283085589465983");
        final OccupationPeriod periodLessons = executionDegree.getPeriodLessons(executionCourse.getExecutionPeriod());
        taskLog("Como é possível?? %s%n", periodLessons != null);

        final OccupationPeriod occupationPeriodForLesson = OccupationPeriod.createOccupationPeriodForLesson(executionCourse,
                new YearMonthDay(2023, 9, 11),
                new YearMonthDay(2024, 01, 05));
        taskLog("Mau!");
    }
}