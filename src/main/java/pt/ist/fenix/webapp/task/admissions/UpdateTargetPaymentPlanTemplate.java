package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

import java.util.HashMap;
import java.util.Map;

public class UpdateTargetPaymentPlanTemplate extends CustomTask {

    @Override
    public void runTask() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("3104827563311194", "3105111031152655"); //Mestrado Bolonha em Engenharia e Gestão da Inovação e Empreendedorismo
        map.put("3104827563311193", "3105111031152655"); //Mestrado Bolonha em Física Médica

        map.forEach((key, value) -> {
            final AdmissionProcessTarget target = FenixFramework.getDomainObject(key);
            final JsonObject outcomeConfigJson = target.getOutcomeConfigJson();
            outcomeConfigJson.addProperty("eventTemplate", value);
            outcomeConfigJson.addProperty("eventTemplateForFirstYear", "290361264046081");
            target.setOutcomeConfig(outcomeConfigJson.toString());
            target.getApplicationSet().stream()
                    .filter(app -> app.getDataObject().has("registration"))
                    .forEach(app -> {
                        final Registration registration = JsonUtils.toDomainObject(app.getDataObject(), "registration");
                        taskLog("Aluno: %s\t%s%n", registration.getNumber(), registration.getPerson().getName());
                    });
        });
    }
}
