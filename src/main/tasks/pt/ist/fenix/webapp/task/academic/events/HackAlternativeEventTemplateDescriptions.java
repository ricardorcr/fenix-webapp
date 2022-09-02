package pt.ist.fenix.webapp.task.academic.events;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.commons.i18n.LocalizedString;

import java.util.Locale;

public class HackAlternativeEventTemplateDescriptions extends WriteCustomTask {

    private static final Locale PT = Locale.forLanguageTag("pt-PT");
    private static final Locale EN = Locale.forLanguageTag("en-GB");

    @Override
    public void runTask() throws Exception {
        Bennu.getInstance().getEventTemplateSet().forEach(eventTemplate -> {
            final LocalizedString title = eventTemplate.getTitle();
            final LocalizedString description = eventTemplate.getDescription()
                    .map(s -> s.replace(
                            "limita as inscrições a um máximo de 42.0 ECTS",
                            "limita as inscrições a um máximo de 70% dos ECTS permitidos para o regime normal"))
                    .map(s -> s.replace(
                            "limits enrolments to a maximum of 42.0 ECTS",
                            "limits enrolments to a maximum of 70% of ECTS allowed for the normal regime."))
                    .map(s -> s.replace(
                            "limita as inscrições a um máximo de 30.0 ECTS",
                            "limita as inscrições a um máximo de 50% dos ECTS permitidos para o regime normal"))
                    .map(s -> s.replace(
                            "limits enrolments to a maximum of 30.0 ECTS",
                            "limits enrolments to a maximum of 50% of ECTS allowed for the normal regime."))
                    .map(s -> s.replace(
                            "Este plano limita as inscrições no ano letivo a 30.0 ECTS.",
                            "Este plano limita as inscrições a um máximo de 50% ECTS permitidos para o regime normal."))
                    .map(s -> s.replace(
                            "This plan limits enrolments for the entire year to 30.0 ECTS.",
                            "This plan limits enrolments for the entire year to to a maximum of 50% of ECTS allowed for the normal regime."))
                    .map(s -> s.replace(
                            "Este plano limita as inscrições no ano letivo a 42.0 ECTS.",
                            "Este plano limita as inscrições a um máximo de 70% ECTS permitidos para o regime normal."))
                    .map(s -> s.replace(
                            "This plan limits enrolments for the entire year to 42.0 ECTS.",
                            "This plan limits enrolments for the entire year to to a maximum of 70% of ECTS allowed for the normal regime."))
                    ;
            taskLog("%s%n%s%n%n%s%n%s%n%n%n%n",
                    title.getContent(PT),
                    title.getContent(EN),
                    description.getContent(PT),
                    description.getContent(EN));
            eventTemplate.setTitle(title);
            eventTemplate.setDescription(description);
        });
    }

}