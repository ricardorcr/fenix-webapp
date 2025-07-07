package pt.ist.fenix.webapp.task.paperPusher.academic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.academic.domain.accounting.AccountingTransaction;
import org.fenixedu.academic.domain.accounting.CustomEvent;
import org.fenixedu.academic.domain.accounting.EventState;
import org.fenixedu.admissions.util.RemoteReader;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.io.domain.GroupBasedFile;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.smartFlow.domain.FlowTemplate;
import org.fenixedu.smartForms.domain.RequestDocument;
import org.fenixedu.smartForms.domain.RequestQueue;
import org.fenixedu.smartForms.domain.RequestType;
import org.fenixedu.smartForms.domain.SmartFormsSystem;
import org.joda.time.DateTime;

import java.io.File;
import java.nio.file.Files;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class InitPhdApplication extends WriteCustomTask implements RemoteReader {

    private String[] testUsers = new String[]{
            "ist24439", "ist24616, ist23068"
    };

    @Override
    public void runTask() throws Exception {
        final SmartFormsSystem smartFormsSystem = SmartFormsSystem.getInstance();

        final Supplier<Stream<RequestQueue>> leafSupplier = () -> smartFormsSystem.getRequestQueueSet().stream()
                .flatMap(RequestQueue::getChildAndSelfStream)
                .filter(queue -> queue.getChildSet().isEmpty());

        leafSupplier.get()
                .filter(queue -> queue.getName().getContent(PT).equals("Serviços Académicos"))
                .flatMap(queue -> queue.getRequestTypeSet().stream())
                .filter(rt -> rt.getName()
                                .anyMatch(l ->
                                                l.equals("Candidatura a Doutoramento")
                                )
                )
                .forEach(rt -> {
                    try {
                        rt.getCurrentRequestTypeVersion().getRequestSet()
                                .forEach(request -> {
                                    if (request.getRequestCost() != null) {
                                        final CustomEvent event = (CustomEvent) request.getRequestCost().getEvent();
                                        if (event != null) {
                                            event.getAccountingTransactionsSet().forEach(AccountingTransaction::delete);
                                            event.forceChangeState(EventState.OPEN.OPEN, new DateTime());
                                            request.getRequestCost().setEvent(null);
                                            event.setCustomToAccount(null);
                                            event.delete();
                                        }
                                        request.getRequestCost().delete();
                                    }
                                    request.setRequestCost(null);
                                    request.setRegistryCode(null);
                                    request.getOutcomeDocumentSet().forEach(RequestDocument::delete);
                                    request.getAttachmentSet().forEach(requestDocument -> {
                                        if (requestDocument.getRequestFile() instanceof GroupBasedFile) {
                                            final GroupBasedFile groupBasedFile = requestDocument.getRequestFile();
                                            groupBasedFile.setReferenceLetterAttachment(null);
                                            requestDocument.delete();
                                        }
                                    });
                                    request.deleteUnchecked();
                                });
                        rt.deleteUnchecked();
                    } catch (Exception | Error ex) {
                        ex.printStackTrace();
                    }
                });
//        SmartFlowSystem.getInstance().getFlowTemplateSet().stream()
//                .filter(flowTemplate -> flowTemplate.getName().equals("phd-application"))
//                .forEach(ft -> {
//                    ft.getFlowQueueSet().clear();
//                    ft.getFlowSet().forEach(flow -> flow.getAttachmentSet().forEach(attachment -> attachment.delete()));
//                    ft.delete();
//                });

        leafSupplier.get()
                .filter(queue -> queue.getName().getContent(PT).equals("Serviços Académicos"))
//                .peek(queue -> Arrays.stream(testUsers).map(User::findByUsername).forEach(queue::addMember))
                .forEach(queue -> {
                    createRequestType(queue, "forms/academic/PhdApplication.json");
                });
    }

    private JsonObject createRequestType(final RequestQueue requestQueue, final String formFilename) {
        final JsonObject data = localObject(formFilename);
        final RequestType requestType = new RequestType(
                LocalizedString.fromJson(data.get("name")),
                LocalizedString.fromJson(data.get("description")),
                data.getAsJsonObject("context"),
                data.getAsJsonObject("inputForm"),
                data.getAsJsonObject("outcomeForm"),
                data.getAsJsonObject("cost"));
        if (requestQueue != null) {
            requestQueue.addRequestType(requestType);
        }
        final JsonObject flowTemplate = data.getAsJsonObject("flowTemplate");
        if (flowTemplate != null) {
            FlowTemplate.createFromJson(flowTemplate);
        }
        return flowTemplate;
    }

    private JsonObject updateRequestType(final String formFilename) {
        final JsonObject data = localObject(formFilename);

        SmartFormsSystem.getInstance().getRequestTypeSet().stream()
                .filter(rt -> rt.getName().equals(LocalizedString.fromJson(data.get("name"))))
                .findAny()
                .ifPresent(rt ->  {
//                    new Requ(rt, data.getAsJsonObject("context"),
//                            data.getAsJsonObject("inputForm"), data.getAsJsonObject("outcomeForm"),
//                            data.getAsJsonObject("cost"));
                });

        final JsonObject flowTemplate = data.getAsJsonObject("flowTemplate");
        if (flowTemplate != null) {
            FlowTemplate.createFromJson(flowTemplate);
        }
        return flowTemplate;
    }

    @Override
    public String baseUrl() {
        return "https://repo.dsi.tecnico.ulisboa.pt/fenixedu/data/-/raw/master/paperPusher/";
    }

    @Override
    public String string(String filename) {
        return RemoteReader.super.string(filename)
                .replaceAll("https://fenix.tecnico.ulisboa.pt", CoreConfiguration.getConfiguration().applicationUrl());
    }

    private static final Locale PT = new Locale("pt", "PT");
    private static final Locale EN = new Locale("en", "GB");

    private static LocalizedString ls(final String pt, final String en) {
        return new LocalizedString(PT, pt).with(EN, en);
    }

    public JsonObject localObject(String filename) {
        try {
            return JsonParser.parseString(new String(Files.readAllBytes(new File("/home/rcro/workspace/data/paperPusher/" + filename).toPath())))
                    .getAsJsonObject();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}