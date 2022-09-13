package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.CustomEvent;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.accounting.EventTemplateConfig;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.util.Bundle;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import pt.ist.fenixframework.FenixFramework;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class CreateGratuityEventForRegistration extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final RegistrationDataByExecutionYear dataByExecutionYear = getRegistrationDataFor("ist1102365");
        final LocalDate enrolmentDate = dataByExecutionYear.getEnrolmentDate();
        final EventTemplate eventTemplate = EventTemplate.templateFor(dataByExecutionYear);
        EventTemplateConfig templateConfig = eventTemplate.getConfigFor(enrolmentDate.toDateTimeAtStartOfDay());
        createEvent(dataByExecutionYear, EventTemplate.Type.TUITION, templateConfig);
    }

    private RegistrationDataByExecutionYear getRegistrationDataFor(final String istID) {
        final User user = User.findByUsername(istID);
        final Registration registration = user.getPerson().getStudent().getActiveRegistrationStream().findAny().get();
        final RegistrationDataByExecutionYear registrationDataByExecutionYear = registration.getRegistrationDataByExecutionYearSet().stream()
                .sorted(Comparator.comparing(RegistrationDataByExecutionYear::getExecutionYear).reversed())
                .findFirst().get();
        return registrationDataByExecutionYear;
    }

    private CustomEvent createEvent(final RegistrationDataByExecutionYear dataByExecutionYear, final EventTemplate.Type type,
                                    final EventTemplateConfig templateConfig) {
        final Registration registration = dataByExecutionYear.getRegistration();
        final ExecutionYear executionYear = dataByExecutionYear.getExecutionYear();
        final JsonObject config = templateConfig.getConfig().getAsJsonObject(type.name());
        final Map<LocalDate, Money> dueDateAmountMap = toDueDateAmountMap(config.getAsJsonObject("dueDateAmountMap"));
        if (dueDateAmountMap.isEmpty()) {
            return null;
        }
        final LocalizedString description = type == EventTemplate.Type.TUITION
                ? BundleUtil.getLocalizedString(Bundle.STUDENT, "label.custom.event." + type.name(),
                registration.getDegree().getSigla(), executionYear.getName())
                : BundleUtil.getLocalizedString(Bundle.STUDENT, "label.custom.event." + type.name(),
                executionYear.getName());
        return createEvent(type, registration, description, dueDateAmountMap,
                data -> {
                    data.addProperty("executionYear", executionYear.getExternalId());
                    data.addProperty("registrationDataByExecutionYear", dataByExecutionYear.getExternalId());
                },
                templateConfig);
    }

    private CustomEvent createEvent(final EventTemplate.Type type, final Registration registration,
                                    final LocalizedString description, final Map<LocalDate, Money> dueDateAmountMap,
                                    final Consumer<JsonObject> eventConfigConsumer,
                                    final EventTemplateConfig templateConfig) {
        final JsonObject config = templateConfig.getConfig().getAsJsonObject(type.name());
        final Person person = registration.getPerson();
        final org.fenixedu.academic.domain.accounting.Account account = FenixFramework.getDomainObject(config.get("accountId").getAsString());
        return new CustomEvent(person, account, dueDateAmountMap, JsonUtils.toJson(data -> {
            data.add("description", description.json());
            data.addProperty("type", type.name());
            final JsonObject penaltyAmountMap = config.getAsJsonObject("penaltyAmountMap");
            if (penaltyAmountMap != null) {
                data.add("penaltyAmountMap", penaltyAmountMap);
            }
            data.add("productCode", config.get("productCode"));
            data.add("productDescription", config.get("productDescription"));

            eventConfigConsumer.accept(data);
        }));
    }

    private Map<LocalDate, Money> toDueDateAmountMap(JsonObject json) {
        Map<LocalDate, Money> dueDateAmountMap = new TreeMap();
        json.entrySet().forEach((e) -> {
            dueDateAmountMap.put(DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(e.getKey()), new Money((e.getValue()).getAsString()));
        });
        return dueDateAmountMap;
    }
}
