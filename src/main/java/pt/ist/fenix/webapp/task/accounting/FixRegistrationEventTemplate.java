package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import pt.ist.fenixframework.FenixFramework;

public class FixRegistrationEventTemplate extends CustomTask {

    @Override
    public void runTask() throws Exception {
        //2º ciclo 1ª fase 24/25 - Ciência de Dados
        final AdmissionProcessTarget target = FenixFramework.getDomainObject("1697452679758037");
        //2º Ciclo Intermédio
        final EventTemplate eventTemplate = FenixFramework.getDomainObject("3105111031152655");
        //2º Ciclo Intermédio - Fase 1
        final EventTemplate eventTemplatePhase = FenixFramework.getDomainObject("3105111031152656");

        target.getApplicationSet().stream()
                .filter(app -> app.getDataObject().has("registration"))
                .forEach(app -> {
                    final Registration registration = JsonUtils.toDomainObject(app.getDataObject(), "registration");
                    if (registration.getRegistrationDataByExecutionYearSet().size() != 1) {
                        taskLog("Hummm: %s%s%n", registration.getPerson().getUsername(), registration.getRegistrationDataByExecutionYearSet().size());
                    }
                    if (registration.getEventTemplate() != eventTemplate) {
                        taskLog("Mudei registration: %s %s\tmain template: %s\tpara %s%n", registration.getExternalId(), registration.getNumber(),
                                registration.getEventTemplate().getDescription().getContent(), eventTemplate.getDescription().getContent());
                        registration.setEventTemplate(eventTemplate);
                    }
                    final RegistrationDataByExecutionYear dataByExecutionYear = registration.getRegistrationDataByExecutionYearSet().iterator().next();
                    if (dataByExecutionYear.getEventTemplate() != eventTemplatePhase) {
                        taskLog("Mudei dataByExecutionYear: %s %s\tspecific template: %s\tpara %s%n", dataByExecutionYear.getExternalId(), registration.getNumber(),
                                dataByExecutionYear.getEventTemplate().getDescription().getContent(), eventTemplatePhase.getDescription().getContent());
                        dataByExecutionYear.setEventTemplate(eventTemplatePhase);
                        taskLog("----------------");
                    }
                });
    }
}
