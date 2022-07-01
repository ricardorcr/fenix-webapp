package pt.ist.fenix.webapp.task;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.messaging.core.domain.Message;
import org.fenixedu.messaging.core.domain.Message_Base;
import pt.ist.fenixframework.FenixFramework;

import java.lang.reflect.Method;

public class HackMessage extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Message message = FenixFramework.getDomainObject("571110391550551");
        final Method method = Message_Base.class.getDeclaredMethod("setTextBody", LocalizedString.class);
        method.setAccessible(true);
        final LocalizedString text = message.getTextBody().map(s -> s.replace("perecer", "parecer"));
        method.invoke(message, text);
    }

}