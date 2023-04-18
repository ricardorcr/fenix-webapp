package pt.ist.fenix.webapp;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;

import java.util.Arrays;
import java.util.List;

public class FixOpenStatePayedEvents extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final List<String> users = Arrays.asList("ist425444");
        users.stream()
                .map(username -> User.findByUsername(username))
                .flatMap(user -> user.getPerson().getEventsSet().stream()
                        .filter(event -> event.isOpen()))
                .forEach(event -> event.recalculateState(new DateTime()));
    }
}
