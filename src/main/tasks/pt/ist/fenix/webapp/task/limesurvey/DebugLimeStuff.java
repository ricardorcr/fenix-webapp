package pt.ist.fenix.webapp.task.limesurvey;

import org.fenixedu.admissions.ist.domain.Survey;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.connect.pluggable.PluggableActivity;

public class DebugLimeStuff extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final User user = User.findByUsername("ist1105708");
        user.getIdentity().getAccountSet().stream()
                .flatMap(a -> a.getApplicationSet().stream())
//                .peek(application -> Survey.updateResponseStatus(application))
                .flatMap(application -> Survey.surveys(application))
                //.filter(survey -> Survey.pendingResponse(survey))
                .forEach(survey -> {
                    taskLog("%s : %s%n", Survey.id(survey), Survey.email(survey));
                    taskLog("   %s%n", survey.toString());
                });

    }

}