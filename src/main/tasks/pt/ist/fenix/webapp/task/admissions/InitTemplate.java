package pt.ist.fenix.webapp.task.admissions;

import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.messaging.core.domain.MessageTemplate;
import org.fenixedu.messaging.core.template.DeclareMessageTemplate;
import org.fenixedu.messaging.core.template.DeclareMessageTemplates;
import org.fenixedu.messaging.core.template.TemplateParameter;
import org.fenixedu.queueing.domain.AttendanceQueue;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;

@DeclareMessageTemplate(id = "queueing.slot.unschedule.confirm.registration", bundle = "resources.QueueingResources",
        description = "queueing.slot.unschedule.description", subject = "queueing.slot.unschedule.subject",
        text = "queueing.slot.unschedule.body",
        parameters = { @TemplateParameter(id = "scheduledStart", description = "queueing.slot.unschedule.scheduledStart"),
                @TemplateParameter(id = "location", description = "queueing.slot.unschedule.location"),
                @TemplateParameter(id = "queue", description = "queueing.slot.unschedule.queue"),
                @TemplateParameter(id = "message", description = "queueing.slot.unschedule.message") })
@DeclareMessageTemplate(id = "queueing.slot.schedule.confirm.registration", bundle = "resources.QueueingResources",
        description = "queueing.slot.schedule.description", subject = "queueing.slot.schedule.subject",
        text = "queueing.slot.schedule.body",
        parameters = { @TemplateParameter(id = "scheduledStart", description = "queueing.slot.schedule.scheduledStart"),
                @TemplateParameter(id = "locationType", description = "queueing.slot.schedule.locationType"),
                @TemplateParameter(id = "location", description = "queueing.slot.schedule.location"),
                @TemplateParameter(id = "queue", description = "queueing.slot.schedule.queue") })
@DeclareMessageTemplate(id = "queueing.slot.reschedule.confirm.registration", bundle = "resources.QueueingResources",
        description = "queueing.slot.reschedule.description", subject = "queueing.slot.reschedule.subject",
        text = "queueing.slot.reschedule.body",
        parameters = { @TemplateParameter(id = "scheduledStart", description = "queueing.slot.reschedule.scheduledStart"),
                @TemplateParameter(id = "locationType", description = "queueing.slot.reschedule.locationType"),
                @TemplateParameter(id = "location", description = "queueing.slot.reschedule.location"),
                @TemplateParameter(id = "queue", description = "queueing.slot.reschedule.queue") })
public class InitTemplate extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        set("853070699298818", "queueing.slot.unschedule.confirm.registration", "queueing.slot.schedule.confirm.registration", "queueing.slot.reschedule.confirm.registration");
        set("853070699298819", "queueing.slot.unschedule.confirm.registration", "queueing.slot.schedule.confirm.registration", "queueing.slot.reschedule.confirm.registration");

        set("1697495629430785", "queueing.slot.unschedule.confirm.identity.validation", "queueing.slot.schedule.identity.validation", "queueing.slot.reschedule.identity.validation");

        set("1978970606141445", "queueing.slot.unschedule.confirm.campus.visit", "queueing.slot.schedule.campus.visit", "queueing.slot.reschedule.campus.visit");
        set("1978970606141444", "queueing.slot.unschedule.confirm.campus.visit", "queueing.slot.schedule.campus.visit", "queueing.slot.reschedule.campus.visit");
        set("1978970606141442", "queueing.slot.unschedule.confirm.campus.visit", "queueing.slot.schedule.campus.visit", "queueing.slot.reschedule.campus.visit");
        set("1416020652720130", "queueing.slot.unschedule.confirm.campus.visit", "queueing.slot.schedule.campus.visit", "queueing.slot.reschedule.campus.visit");
        set("1978970606141443", "queueing.slot.unschedule.confirm.campus.visit", "queueing.slot.schedule.campus.visit", "queueing.slot.reschedule.campus.visit");
        set("1978970606141446", "queueing.slot.unschedule.confirm.campus.visit", "queueing.slot.schedule.campus.visit", "queueing.slot.reschedule.campus.visit");
    }

    private void set(final String id, final String unschedule, final String schedule, final String reschedule) {
        Arrays.stream(this.getClass().getAnnotationsByType(DeclareMessageTemplates.class))
                .flatMap(t -> Arrays.stream(t.value()))
                .forEach(MessageTemplate::declare);

        final AttendanceQueue queue = FenixFramework.getDomainObject(id);
        queue.setEmailTemplateUnSchedule(unschedule);
        queue.setEmailTemplateReSchedule(reschedule);
        queue.setEmailTemplateSchedule(schedule);
    }

}