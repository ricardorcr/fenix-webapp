package pt.ist.fenix.webapp.task.paperPusher.academic;

import com.google.gson.JsonObject;
import org.fenixedu.bennu.core.json.ImmutableJsonElement;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.referenceLetters.domain.ReferenceLetterAttachment;
import org.fenixedu.referenceLetters.domain.ReferenceLetterRequest;
import org.fenixedu.referenceLetters.domain.ReferenceLetterScope;
import org.fenixedu.smartFlow.domain.Flow;
import org.fenixedu.smartForms.domain.Request;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.backend.jvstmojb.pstm.AbstractDomainObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class DeletePhdRequestFlow extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final Request request = FenixFramework.getDomainObject("853491606097398");
        request.getAttachmentSet().stream()
                .filter(document -> document.getField().endsWith("CURRICULUM"))
                .forEach(document -> {
                    final ReferenceLetterAttachment letterAttachment = document.getRequestFile().getReferenceLetterAttachment();
                    final ReferenceLetterScope letterScope = letterAttachment.getScope();
                    letterScope.getRequestSet().forEach(ReferenceLetterRequest::delete);
                    letterAttachment.setFile(null);
                    letterAttachment.setScope(null);
                    try {
                        final Method method = AbstractDomainObject.class.getDeclaredMethod("deleteDomainObject");
                        method.setAccessible(true);
                        method.invoke(letterAttachment);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                    letterScope.setReferenceLettersSystem(null);
                    try {
                        final Method method = AbstractDomainObject.class.getDeclaredMethod("deleteDomainObject");
                        method.setAccessible(true);
                        method.invoke(letterScope);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }

                });

        final JsonObject data = request.getData().get();
        final String flowId = data.get("flowId").getAsString();
        final Flow flow = FenixFramework.getDomainObject(flowId);
        flow.delete();

        data.remove("flowId");
        request.setData(new ImmutableJsonElement<>(data));
    }
}
