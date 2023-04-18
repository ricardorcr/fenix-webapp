package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonObject;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FixSpecificRegimeAnswer extends CustomTask {

    LocalizedString answer = null;
    @Override
    public void runTask() throws Exception {
        Locale PT = Locale.forLanguageTag("pt");
        Locale EN = Locale.forLanguageTag("en");
        answer = new LocalizedString(PT, "Quero ingressar por via de um regime ou acordo específico");
        answer = answer.with(EN,"I want to apply via a specific regiment or agreement");

        final List<String> processIDs = Arrays.asList("571432513831037", "852907490541573", "852907490541574", "852907490541572", "852907490541575");
        processIDs.stream()
                .map(oid -> (AdmissionProcess) FenixFramework.getDomainObject(oid))
                .forEach(this::fix);
    }

    private void fix(final AdmissionProcess admissionProcess) {
        final JsonObject outcomeConfig = admissionProcess.getOutcomeConfigJson();
        final JsonObject type = outcomeConfig.get("type").getAsJsonObject();
        type.add("answer", answer.json());
        admissionProcess.setOutcomeConfig(outcomeConfig.toString());
    }
}
