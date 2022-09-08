package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.Survey;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import pt.ist.fenixframework.FenixFramework;

public class ForceSurveyUpdate extends WriteCustomTask {
    @Override
    public void runTask() throws Exception {
        final Application application = FenixFramework.getDomainObject("571415333968005");
        Survey.updateResponseStatus(application);
    }
}