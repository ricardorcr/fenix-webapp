package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.ist.domain.RegistrationProcessState;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.admissions.ist.wizard.RemoteReader;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class InitOutcomeFormsAndActivitiesForRaides extends CustomTask implements RemoteReader {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(process -> Utils.isDegreeType(process)
                        || Utils.isDegreeSpecificRegimentType(process)
                        || Utils.isReinstatement(process))
                .filter(process -> !Utils.isDges(process))
                .forEach(process -> {
                    final JsonObject outcomeConfig = process.getOutcomeConfigJson();
                    JsonObject forms = outcomeConfig.getAsJsonObject("forms");
                    if (forms == null) {
                        forms = new JsonObject();
                        outcomeConfig.add("forms", forms);
                    }
                    JsonObject afterOutcome = forms.getAsJsonObject("afterOutcome");
                    if (afterOutcome == null) {
                        final JsonObject raides = object("raidesForm.json");
                        final String localRaides = raides.toString().replaceAll("https://fenix.tecnico.ulisboa.pt",
                                CoreConfiguration.getConfiguration().applicationUrl());
                        forms.add("afterOutcome", JsonParser.parseString(localRaides));
                    }
                    process.setOutcomeConfig(outcomeConfig.toString());

                    process.getAdmissionProcessTargetSet().stream()
                            .flatMap(target -> target.getApplicationSet().stream())
                            .filter(application -> Utils.outcomeStateFor(application) == RegistrationProcessState.REGISTERED)
                            .forEach(application -> {
                                final JsonObject data = application.getDataObject();
                                final JsonObject outcomeState = data.getAsJsonObject("outcomeState");
                                outcomeState.addProperty("canEditPostOutcomeForm", true);
                                application.setData(data.toString());
                            });
                });
    }

}