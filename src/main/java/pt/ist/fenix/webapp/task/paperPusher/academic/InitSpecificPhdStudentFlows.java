package pt.ist.fenix.webapp.task.paperPusher.academic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.admissions.util.RemoteReader;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.commons.stream.StreamUtils;
import org.fenixedu.smartFlow.domain.Flow;
import org.fenixedu.smartFlow.domain.FlowLog;
import org.fenixedu.smartFlow.domain.FlowQueue;
import org.fenixedu.smartFlow.domain.FlowState;
import org.fenixedu.smartFlow.domain.FlowTemplate;
import org.fenixedu.smartFlow.domain.SmartFlowSystem;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InitSpecificPhdStudentFlows extends CustomTask implements RemoteReader {

    private static final Locale PT = new Locale("pt");
    private static final Locale EN = new Locale("en");

    private static LocalizedString ls(final String pt, final String en) {
        return new LocalizedString(PT, pt).with(EN, en);
    }

    @Override
    public void runTask() throws Exception {
        final JsonObject config = object("StudentRegistrationFlow.json");
        final JsonObject flowTemplateConfig = config.getAsJsonObject("flowTemplate");

        final String tempalteName = flowTemplateConfig.get("name").getAsString();
        final FlowTemplate flowTemplate = SmartFlowSystem.getTemplateBy(tempalteName)
                .orElseThrow(() -> new Error("Not Found flow template " + tempalteName));

        Stream.of("ist1115066", "ist1115067", "ist1115072")
                .map(User::findByUsername)
                .forEach(user -> {
                    Set<Registration> registrations = user.getPerson().getStudent().getRegistrationsSet().stream()
                            .filter(Registration::isDEA)
                            .collect(Collectors.toSet());
                    if (registrations.size() != 1) {
                        taskLog("menino maroto: %s%n", user.getUsername());
                    } else {
                        final Registration registration = registrations.iterator().next();
                        final FlowQueue flowQueue = SmartFlowSystem.getUserQueue(user);
                        if (flowQueue == null || flowQueue.getFlowSet().isEmpty()) {
                            final JsonObject flowData = toFlowDataPhd(config, registration, flowTemplate);
                            final Flow flow = new Flow(flowTemplate, user, flowData);
                            flow.getFlowLogSet().forEach(FlowLog::delete);
                            flow.setState(FlowState.RUNNING);
                            try {
                                Authenticate.mock(user, "InitPhdFlow");
                                flow.doAction("NEXT", new JsonObject().toString());
                            } finally {
                                Authenticate.unmock();
                            }
                        }
                    }
                });
    }

    private JsonObject toFlowDataPhd(final JsonObject config, final Registration registration, final FlowTemplate flowTemplate) {
        final User user = registration.getPerson().getUser();
        final FlowQueue flowQueue = SmartFlowSystem.getUserQueue(user);

        final JsonObject flowData = flowTemplate.getConfig().get();
        flowData.add("viewForm", config.getAsJsonObject("viewForm"));
        final JsonObject viewData = new JsonObject();
        viewData.add("0", new JsonObject());
        flowData.add("viewData", viewData);
        final JsonArray keywords = new JsonArray();
        if (registration.getIngressionType() != null) {
            keywords.add(ls(registration.getIngressionType().getLocalizedName(PT), registration.getIngressionType().getLocalizedName(EN)).json());
        } else {
            keywords.add(ls("PHD", "PHD").json());
        }
        keywords.add(registration.getDegree().getNameI18N().json());
        JsonUtils.addIf(flowData, "keywords", keywords);
        final String bennuApplicationUrl = CoreConfiguration.getConfiguration().applicationUrl();
        String data = flowData.toString()
                .replaceAll("\\{\\{username\\}\\}", flowQueue.getName())
                .replaceAll("\\{\\{bennuApplicationUrl\\}\\}", bennuApplicationUrl);

        final JsonObject result = JsonParser.parseString(data).getAsJsonObject();
        final JsonArray actionNodes = result.getAsJsonObject("config").getAsJsonArray("actionNodes");
        final JsonObject nodeStudentCard = get(actionNodes, "student-card");
        final JsonObject nodePostOutcomeForm = get(actionNodes, "postOutcome-form");
        actionNodes.remove(nodePostOutcomeForm);

        final JsonObject nodeSurvey = get(actionNodes, "survey");
        final JsonObject nodeConfirmDocuments = get(actionNodes, "confirm-documents");
        actionNodes.remove(nodeSurvey);
        actionNodes.remove(nodeConfirmDocuments);

        setNextStep(nodeStudentCard, "SUCCESS");

        result.addProperty("registration", registration.getExternalId());

        return result;
    }

    private void setNextStep(final JsonObject previous, final String nextId) {
        previous.getAsJsonObject("actions").entrySet().stream()
                .map(e -> e.getValue().getAsJsonObject())
                .forEach(action -> action.addProperty("to", nextId));

    }

    private JsonObject get(final JsonArray actionNodes, final String nodeId) {
        return StreamUtils.of(actionNodes)
                .map(JsonElement::getAsJsonObject)
                .filter(node -> node.get("id").getAsString().equals(nodeId))
                .findAny().orElseThrow(() -> new Error("Unable to find node with id " + nodeId));
    }

    @Override
    public String baseUrl() {
        return "https://repo.dsi.tecnico.ulisboa.pt/fenixedu/data/-/raw/master/paperPusher/flows/";
    }

}
