package pt.ist.fenix.webapp.task.accounting;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.accounting.CustomEvent;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixframework.FenixFramework;

public class CheckInvalidRegistrationDataEvent extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        //TODO escolher o evento que tem menos por pagar, meter o rdey certo nesse e isentar o outro quando têm o mesmo nome
        //TODO se tiver matrícula correspondente anulada, isentar o resto da dívida
        Spreadsheet spreadsheet = new Spreadsheet("Eventos");
        Bennu.getInstance().getAccountingEventsSet().stream()
                .filter(CustomEvent.class::isInstance)
                .map(CustomEvent.class::cast)
                .filter(event -> event.getConfigObject().has("executionYear"))
                .forEach(event -> {
                    final ExecutionYear executionYear = JsonUtils.toDomainObject(event.getConfigObject(), "executionYear");
                    if (executionYear.isCurrent()) {
                        if (event.getConfigObject().has("registrationDataByExecutionYear")) {
                            final RegistrationDataByExecutionYear rdey = JsonUtils.toDomainObject(event.getConfigObject(), "registrationDataByExecutionYear");
                            if (rdey == null || !FenixFramework.isDomainObjectValid(rdey)) {
                                Spreadsheet.Row row = spreadsheet.addRow();
                                row.setCell("OID", event.getExternalId());
                                row.setCell("IstID", event.getPerson().getUsername());
                                row.setCell("Descrição", event.getDescriptionI18N().getContent());
                                row.setCell("Estado", event.getEventState().getName());
                                row.setCell("Estado Matrícula", getRegistrationState(event));
                                reportTuitionForSameYear(executionYear, event, row);
                            }
                        }
                    }
                });
        output("eventos_registration_data_inconsistente.xlsx", spreadsheet.exportToXLSXSheet());
    }

    private String getRegistrationState(final CustomEvent event) {
        final Registration registration = event.getPerson().getStudent().getRegistrationsSet().stream()
                .filter(r -> !r.getDegree().isEmpty())
                .filter(r -> event.getDescriptionI18N().getContent().contains(r.getDegree().getSigla()))
                .peek(r -> taskLog("foneix! %s\t%s\t%s%n", r.getExternalId(), r.getDegree().getSigla(), event.getPerson().getUsername()))
                .findAny().orElse(null);
        return registration != null ? registration.getActiveState().getStateType().getName() : "";
    }

    private void reportTuitionForSameYear(final ExecutionYear executionYear, final CustomEvent tuitionEvent, final Spreadsheet.Row row) {
        tuitionEvent.getPerson().getEventsSet().stream()
                .filter(event -> event != tuitionEvent)
                .filter(CustomEvent.class::isInstance)
                .map(CustomEvent.class::cast)
                .filter(CustomEvent::isGratuity)
                .filter(event -> JsonUtils.toDomainObject(event.getConfigObject(), "executionYear") == executionYear)
                .forEach(event -> {
                    row.setCell("Outro Evento", event.getDescriptionI18N().getContent());
                    row.setCell("Outro Estado", event.getEventState().getName());
                });
    }
}
