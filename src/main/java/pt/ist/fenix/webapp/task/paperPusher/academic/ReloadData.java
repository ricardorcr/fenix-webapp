package pt.ist.fenix.webapp.task.paperPusher.academic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.admissions.util.RemoteReader;
import org.fenixedu.bennu.core.json.ImmutableJsonElement;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.smartFlow.domain.FlowTemplate;
import org.fenixedu.smartFlow.domain.SmartFlowSystem;
import org.fenixedu.smartForms.domain.RequestType;
import org.fenixedu.smartForms.domain.SmartFormsSystem;

import java.io.File;
import java.nio.file.Files;

public class ReloadData extends WriteCustomTask implements RemoteReader {

    @Override
    public void runTask() throws Exception {
        for (final String formFilename : FORMS) {
            final JsonObject data = localObject(formFilename);
            final LocalizedString requestName = LocalizedString.fromJson(data.get("name"));

            SmartFormsSystem.getInstance().getRequestTypeSet().stream()
                    .filter(requestType -> requestType.getName().equals(requestName))
                    .forEach(requestType -> reload(requestType, data));
        }
    }

    private void reload(final RequestType requestType, final JsonObject data) {
        requestType.setDescription(LocalizedString.fromJson(data.get("description")));
        requestType.getCurrentRequestTypeVersion().setContext(ImmutableJsonElement.of(data.getAsJsonObject("context")));
        requestType.getCurrentRequestTypeVersion().setInputForm(ImmutableJsonElement.of(data.getAsJsonObject("inputForm")));
        requestType.getCurrentRequestTypeVersion().setOutcomeForm(ImmutableJsonElement.of(data.getAsJsonObject("outcomeForm")));
        requestType.getCurrentRequestTypeVersion().setCost(ImmutableJsonElement.of(data.getAsJsonObject("cost")));

        final JsonObject templateJson = data.getAsJsonObject("flowTemplate");
        if (templateJson != null) {
            final String name = templateJson.get("name").getAsString();
            final FlowTemplate flowTemplate = SmartFlowSystem.getTemplateBy(name).get();
            final LocalizedString title = LocalizedString.fromJson(templateJson.get("title"));
            final LocalizedString description = LocalizedString.fromJson(templateJson.get("description"));
            final JsonObject config = templateJson.getAsJsonObject("config");
            flowTemplate.setTitle(title);
            flowTemplate.setDescription(description);
            flowTemplate.setConfig(ImmutableJsonElement.of(config));
        }
    }

    private static String[] FORMS = new String[]{
//            "forms/academic/CertificateOfRegistrationRequestFormData.json",
//            "forms/academic/FormalDiplomaRequestFormData.json",
            "forms/academic/DiplomaSupplementRequestFormData.json",
//            "forms/academic/DegreeCertificateRequestFormData.json",
//            "forms/academic/DuplicateDocumentFormData.json",
//            "forms/academic/RegisterComplementaryInformationRequestFormData.json"
//            "forms/academic/FreeRequestFormData.json",
//            "forms/academic/ProgrammesAndCoursesWorkloadsCertificateFormData.json",
//            "forms/academic/CompleteCurricularInformationFormData.json",
//            "forms/academic/TranscriptOfRecordsRequestFormData.json"
    };

    @Override
    public String baseUrl() {
        return "https://repo.dsi.tecnico.ulisboa.pt/fenixedu/data/-/raw/master/paperPusher/";
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