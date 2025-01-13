package pt.ist.fenix.webapp.task.academic;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.EvaluationSeason;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class ChangeEnrolmentSeason extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Enrolment enrolment = FenixFramework.getDomainObject("3097650672959577");
        enrolment.setEvaluationSeason(EvaluationSeason.readExtraordinarySeason());
    }
}
