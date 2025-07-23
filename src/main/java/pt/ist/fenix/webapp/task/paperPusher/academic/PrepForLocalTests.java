package pt.ist.fenix.webapp.task.paperPusher.academic;

import com.google.gson.JsonParser;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.json.ImmutableJsonElement;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.smartFlow.domain.SmartFlowSystem;
import org.fenixedu.smartForms.domain.SmartFormsSystem;

import java.util.HashSet;
import java.util.Set;

public class PrepForLocalTests extends WriteCustomTask {

    final Set<String> USERNAMES = new HashSet<>();
    {
        USERNAMES.add("ist24616");
    }

    @Override
    public void runTask() throws Exception {
        SmartFormsSystem.getInstance().getRequestTypeSet().forEach(requestType -> {
            final String inputForm = requestType.getCurrentRequestTypeVersion().getInputForm().get().toString()
                    .replaceAll("https://fenix.tecnico.ulisboa.pt",
                            CoreConfiguration.getConfiguration().applicationUrl());
            requestType.getCurrentRequestTypeVersion().setInputForm(new ImmutableJsonElement<>(JsonParser.parseString(inputForm).getAsJsonObject()));
            final String outcomeForm = requestType.getCurrentRequestTypeVersion().getOutcomeForm().get().toString()
                    .replaceAll("https://fenix.tecnico.ulisboa.pt",
                            CoreConfiguration.getConfiguration().applicationUrl());
            requestType.getCurrentRequestTypeVersion().setOutcomeForm(new ImmutableJsonElement<>(JsonParser.parseString(outcomeForm).getAsJsonObject()));

            requestType.getCurrentRequestTypeVersion().getRequestSet()
                    .forEach(request -> {
                        final String data = request.getData().get().toString().replaceAll("https://fenix.tecnico.ulisboa.pt",
                                CoreConfiguration.getConfiguration().applicationUrl());
                        request.setData(new ImmutableJsonElement<>(JsonParser.parseString(data).getAsJsonObject()));
                    });
        });

        AdmissionsSystem.getInstance().getAdmissionProcessSet().forEach(admissionProcess -> {
            admissionProcess.setFormData(admissionProcess.getFormData().replaceAll("https://fenix.tecnico.ulisboa.pt", CoreConfiguration.getConfiguration().applicationUrl()));
            admissionProcess.setOutcomeConfig(admissionProcess.getOutcomeConfig().replaceAll("https://fenix.tecnico.ulisboa.pt", CoreConfiguration.getConfiguration().applicationUrl()));
        });

        SmartFlowSystem.getInstance().getFlowQueueSet().stream()
                .filter(flowQueue -> !flowQueue.getName().startsWith("ist") && flowQueue.getName().indexOf(" ist") < 0)
                .forEach(flowQueue -> USERNAMES.forEach(u -> flowQueue.addMember(User.findByUsername(u))));

    }

}
