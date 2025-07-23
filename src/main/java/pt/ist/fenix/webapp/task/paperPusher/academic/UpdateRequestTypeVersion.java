package pt.ist.fenix.webapp.task.paperPusher.academic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.fenixedu.admissions.util.RemoteReader;
import org.fenixedu.bennu.core.json.ImmutableJsonElement;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.smartFlow.domain.FlowTemplate;
import org.fenixedu.smartFlow.domain.SmartFlowSystem;
import org.fenixedu.smartForms.domain.RequestQueue;
import org.fenixedu.smartForms.domain.RequestType;
import org.fenixedu.smartForms.domain.RequestTypeVersion;
import org.fenixedu.smartForms.domain.SmartFormsSystem;
import org.joda.time.DateTime;

public class UpdateRequestTypeVersion extends WriteCustomTask implements RemoteReader {

    private static String[] FORMS = new String[]{
//            "humanResources/RemoteWork.json",
//            "humanResources/Overtime.json",
//            "humanResources/ContractTermination.json",
            "academic/PhdApplication.json"
    };
    
    private static String[] FLOWS = new String[]{
//            "humanResources/OvertimeRecord.json",
//            "humanResources/ScholarshipBoardingFlow.json"
  };


    @Override
    public void runTask() throws Exception {
        Arrays.stream(FORMS).forEach(filename -> createRequestType(null, filename));
//        Arrays.stream(FLOWS).forEach(filename -> createFlowTemplate(null, filename));
    }

    private JsonObject createRequestType(final RequestQueue requestQueue, final String formFilename) {
 
        final JsonObject data = object(formFilename);
        final LocalizedString name = LocalizedString.fromJson(data.get("name"));
        final RequestType requestType = SmartFormsSystem.getInstance().getRequestTypeSet().stream()
                .filter(type -> type.getName().equals(name))
                .findAny().orElseThrow(() -> new RuntimeException());        
        RequestTypeVersion currentRequestTypeVersion = requestType.getCurrentRequestTypeVersion();

        currentRequestTypeVersion.setContext(ImmutableJsonElement.of(data.getAsJsonObject("context")));
        currentRequestTypeVersion.setInputForm(ImmutableJsonElement.of( data.getAsJsonObject("inputForm")));
        currentRequestTypeVersion.setOutcomeForm(ImmutableJsonElement.of(data.getAsJsonObject("outcomeForm")));
        currentRequestTypeVersion.setCost(ImmutableJsonElement.of(data.getAsJsonObject("cost")));
        
        return createFlowTemplate(requestQueue, formFilename);
          
    }

    private JsonObject createFlowTemplate(final RequestQueue requestQueue, final String formFilename) {
        
        final JsonObject data = object(formFilename);
        final JsonObject templateJson = data.getAsJsonObject("flowTemplate");
        if (templateJson != null) {
            final String name = templateJson.get("name").getAsString();
            Optional<FlowTemplate> flowTemplate = SmartFlowSystem.getInstance().getFlowTemplateSet().stream().filter(ft -> ft.getName().equals(name)).findAny();
            if (flowTemplate.isPresent()) {
                final LocalizedString title = LocalizedString.fromJson(templateJson.get("title"));
                final LocalizedString description = LocalizedString.fromJson(templateJson.get("description"));
                final JsonObject config = templateJson.getAsJsonObject("config");
                flowTemplate.get().setTitle(title);
                flowTemplate.get().setDescription(description);
                flowTemplate.get().setConfig(ImmutableJsonElement.of(config));
            } else {
                FlowTemplate.createFromJson(templateJson);
            }
        }
       
        return templateJson;
    }
     
    @Override
    public String baseUrl() {
        return "https://repo.dsi.tecnico.ulisboa.pt/fenixedu/data/-/raw/master/paperPusher/forms/";
    }

}