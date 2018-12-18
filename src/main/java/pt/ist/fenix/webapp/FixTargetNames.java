package pt.ist.fenix.webapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.organizationalStructure.UniversityUnit;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.ist.wizard.CreateInboundMobilityProcesses;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.commons.i18n.LocalizedString;
import pt.ist.fenixframework.FenixFramework;

import java.util.Locale;

import static org.fenixedu.academic.util.LocaleUtils.EN;
import static org.fenixedu.academic.util.LocaleUtils.PT;

public class FixTargetNames extends CustomTask {

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
            .filter(ap -> ap.getCreationTemplate() != null)
            .filter(ap -> ap.getCreationTemplate() instanceof CreateInboundMobilityProcesses)
            .flatMap(ap -> ap.getAdmissionProcessTargetSet().stream())
            .filter(this::isToFix)
            .forEach(this::fix);
    }

    private void fix(AdmissionProcessTarget target) {
        taskLog("%s\t%s%n", target.getExternalId(), target.getName().toString());
        String ptPTContent = target.getName().getContent(PT);
        String enGBContent = target.getName().getContent(EN);
        if (target.getName().getContent(PT).contains("()")) {
            ptPTContent = target.getName().getContent(PT).replace("()", "(" + getName(target, PT) + ")");
        }
        if (target.getName().getContent(EN).contains("()")) {
            enGBContent = target.getName().getContent(EN).replace("()", "(" + getName(target, EN) + ")");
        }
        final LocalizedString correctLS = new LocalizedString(PT, ptPTContent).with(EN, enGBContent);
        target.setName(correctLS);

        final JsonArray tagsConfig = JsonUtils.parseJsonArray(target.getTagsConfig());
        tagsConfig.forEach(json -> {
            final JsonObject jsonObject = json.getAsJsonObject();
            final String name = jsonObject.get("name").getAsString();
            UniversityUnit univ = FenixFramework.getDomainObject(name);
            if (univ != null) {
                jsonObject.add("title", correctLS.json());
            }
        });
        target.setTagsConfig(tagsConfig.toString());
    }

    private String getName(AdmissionProcessTarget target, Locale locale) {
        final String univID = target.getTags().get(0).get("name").getAsString();
        UniversityUnit univ = FenixFramework.getDomainObject(univID);
        return univ.getNameI18n().getContent(locale) != null ? univ.getNameI18n().getContent(locale) : univ.getNameI18n().getContent();
    }

    private boolean isToFix(AdmissionProcessTarget target) {
        return target.getName().getLocales().stream()
                .anyMatch(locale -> target.getName().getContent(locale).contains("()"));
    }
}
