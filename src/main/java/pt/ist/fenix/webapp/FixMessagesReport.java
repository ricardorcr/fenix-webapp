package pt.ist.fenix.webapp;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.messaging.core.domain.MessagingSystem;

import pt.ist.fenixframework.FenixFramework;

public class FixMessagesReport extends CustomTask {

    @Override
    public void runTask() throws Exception {
       Bennu.getInstance().getMessagingSystem().getMessageSet().stream()
           .filter(m -> m.getDispatchReport() == null)
           .filter(m -> m.getCreated().getYear() == 2020)
           .forEach(m -> 
               FenixFramework.atomic(() -> {
                   taskLog("Dispatching message: %s\n", m.getExternalId());
                   MessagingSystem.dispatch(m);
               })
           );
    }
}
