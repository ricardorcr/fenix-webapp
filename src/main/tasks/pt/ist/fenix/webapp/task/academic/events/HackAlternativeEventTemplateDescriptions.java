package pt.ist.fenix.webapp.task.academic.events;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

import java.util.Locale;

public class HackAlternativeEventTemplateDescriptions extends ReadCustomTask {

    private static final Locale PT = Locale.forLanguageTag("pt-PT");
    private static final Locale EN = Locale.forLanguageTag("en-GB");

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getEventTemplateSet().forEach(eventTemplate -> {
            taskLog("%s%n%s%n%n%s%n%s%n%n%n%n",
                    eventTemplate.getTitle().getContent(PT),
                    eventTemplate.getTitle().getContent(EN),
                    eventTemplate.getDescription().getContent(PT),
                    eventTemplate.getDescription().getContent(EN));
        });
    }

}