package pt.ist.fenix.webapp.task.academic;

import com.google.gson.JsonObject;
import org.fenixedu.admissions.util.RemoteReader;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.smartFlow.domain.FlowTemplate;
import org.fenixedu.smartFlow.domain.SmartFlowSystem;
import org.fenixedu.smartForms.domain.RequestQueue;
import org.fenixedu.smartForms.domain.RequestType;
import org.fenixedu.smartForms.domain.SmartFormsSystem;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class InitAcademicForms extends WriteCustomTask implements RemoteReader {

    private String[] testUsers = new String[]{

            "ist24439", "ist24884", "ist12048", "ist177966", "ist31302", "ist14152", "ist23978", "ist23487",
            "ist23674", "ist24588", "ist24506", "ist173882", "ist24616"

    };

    @Override
    public void runTask() throws Exception {
        deleteAll();

        final SmartFormsSystem smartFormsSystem = SmartFormsSystem.getInstance();

        final Supplier<Stream<RequestQueue>> leafSupplier = () -> smartFormsSystem.getRequestQueueSet().stream()
                .flatMap(RequestQueue::getChildAndSelfStream)
                .filter(queue -> queue.getChildSet().isEmpty());

        leafSupplier.get()
                .filter(queue -> queue.getName().getContent(PT).equals("Serviços Académicos"))
//                .peek(queue -> Arrays.stream(testUsers).map(User::findByUsername).forEach(queue::addMember))
                .forEach(queue -> Arrays.stream(FORMS).forEach(filename -> createRequestType(queue, filename)));
    }

    private static String[] FORMS = new String[]{
/*
            "forms/academic/CertificateOfRegistrationRequestFormData.json",
            "forms/academic/FormalDiplomaRequestFormData.json",
            "forms/academic/DiplomaSupplementRequestFormData.json",
            "forms/academic/DegreeCertificateRequestFormData.json",
            "forms/academic/DuplicateDocumentFormData.json"
            "forms/academic/CompleteCurricularInformationFormData.json"
//            "forms/academic/RegisterComplementaryInformationRequestFormData.json",
 */


            "forms/academic/FreeRequestFormData.json",
            "forms/academic/ProgrammesAndWorkloadsCertificateFormData.json"
//            "forms/academic/TranscriptOfRecordsRequestFormData.json"
//            "forms/academic/CompleteCurricularInformationFormData.json"
    };

    private void deleteAll() {
        for (final String formFilename : FORMS) {
            final JsonObject data = object(formFilename);
            final LocalizedString requestName = LocalizedString.fromJson(data.get("name"));
            SmartFormsSystem.getInstance().getRequestTypeSet().stream()
                    .filter(requestType -> requestType.getName().equals(requestName))
                    .flatMap(requestType -> requestType.getCurrentRequestTypeVersion().getRequestSet().stream())
                    .peek(request -> request.setRegistryCode(null))
                    .map(request -> request.getRequestCost())
                    .filter(Objects::nonNull)
                    .forEach(requestCost -> requestCost.setEvent(null));
            SmartFormsSystem.getInstance().getRequestTypeSet().stream()
                    .filter(requestType -> requestType.getName().equals(requestName))
                    .forEach(RequestType::deleteUnchecked);

            final JsonObject flowTemplate = data.getAsJsonObject("flowTemplate");
            if (flowTemplate != null) {
                final String flowName = flowTemplate.get("name").getAsString();
                SmartFlowSystem.getInstance().getFlowTemplateSet().stream()
                        .filter(ft -> ft.getName().equals(flowName))
                        .forEach(FlowTemplate::delete);
            }
        }
    }

    private JsonObject createRequestType(final RequestQueue requestQueue, final String formFilename) {
        final JsonObject data = object(formFilename);
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

    @Override
    public String baseUrl() {
        return "https://repo.dsi.tecnico.ulisboa.pt/fenixedu/data/-/raw/master/paperPusher/";
    }

    @Override
    public String string(String filename) {
        return RemoteReader.super.string(filename)
                .replaceAll("https://fenix.tecnico.ulisboa.pt", CoreConfiguration.getConfiguration().applicationUrl());
    }

    private static final Locale PT = new Locale("pt");
    private static final Locale EN = new Locale("en");

    private static LocalizedString ls(final String pt, final String en) {
        return new LocalizedString(PT, pt).with(EN, en);
    }

}