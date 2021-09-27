package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

import java.util.stream.Collectors;

public class CheckTransition extends CustomTask {

    @Override
    public void runTask() throws Exception {
        DateTime deploy = new DateTime(2021,9,7,05,0,0);
        ExecutionYear.readCurrentExecutionYear().getNextExecutionYear().getDegreeCurricularTransitionPlanFromDestinationSet().stream()
                .flatMap(dt -> dt.getStudentDegreeCurricularTransitionPlanSet().stream())
                .filter(studentPlan -> studentPlan.getConfirmTransitionInstant() != null
                        && studentPlan.getConfirmTransitionInstant().isAfter(deploy)).collect(Collectors.toSet())
                .forEach(studentPlan -> {
                    final Person person = studentPlan.getStudent().getPerson();
                    taskLog("%s\t%s\t%s%n", person.getUsername(), person.getName(), studentPlan.getDegreeCurricularTransitionPlan().getDestinationDegreeCurricularPlan().getName());
                });
    }
}
