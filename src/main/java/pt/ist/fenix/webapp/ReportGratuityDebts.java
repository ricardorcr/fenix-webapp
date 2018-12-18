package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.events.gratuity.GratuityEvent;
import org.fenixedu.academic.domain.phd.PhdProgram;
import org.fenixedu.academic.domain.phd.debts.PhdGratuityEvent;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

public class ReportGratuityDebts extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2019/2020");
        final Spreadsheet spreadsheet = new Spreadsheet("Propinas 2019-2020");
        Bennu.getInstance().getAccountingEventsSet().stream()
            .filter(GratuityEvent.class::isInstance)
            .map(e -> (GratuityEvent)e)
            .filter(e -> e.getExecutionYear() == executionYear)
            .forEach(e -> report(e, spreadsheet, executionYear));
        
        Bennu.getInstance().getAccountingEventsSet().stream()
            .filter(PhdGratuityEvent.class::isInstance)
            .map(e -> (PhdGratuityEvent)e)
            .filter(e -> ExecutionYear.getExecutionYearByDate(e.getWhenOccured().toYearMonthDay()) == executionYear)
            .forEach(e -> report(e, spreadsheet, executionYear));
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("Propinas_2019_2020.xls", baos.toByteArray());
    }
    
    private void report(PhdGratuityEvent event, Spreadsheet spreadsheet, ExecutionYear executionYear) {
        if (event.isCancelled() && event.getSapRequestSet().isEmpty()) {
            return;
        }
        Row row = spreadsheet.addRow();        
        row.setCell("EventoID", event.getExternalId());
        row.setCell("Evento", event.getDescription().toString());
        row.setCell("Cancelado", event.isCancelled() ? "Sim" : "Não");
        row.setCell("Dívida original", event.getOriginalAmountToPay().getAmountAsString());
        row.setCell("Dívida actual", event.getAmountToPay().getAmountAsString());
        PhdProgram phdProgram = event.getPhdIndividualProgramProcess().getPhdProgram();
        row.setCell("Curso", phdProgram.getPresentationName(executionYear));
        row.setCell("Ciclo", "3º ciclo");
        boolean isAlameda = phdProgram.getDegree().getCampus(executionYear).stream().anyMatch(c -> c.getName().contains("Alameda"));
        row.setCell("Campus", isAlameda ? "Alameda" : "Tagus");
        row.setCell("Regime", RegistrationRegimeType.FULL_TIME.toString());
        row.setCell("istID", event.getPerson().getUsername());
        row.setCell("Nome", event.getPerson().getName());
        row.setCell("NIF", event.getPerson().getSocialSecurityNumber());
        row.setCell("Nacionalidade", event.getPerson().getCountry().getName());
    }

    private void report(final GratuityEvent event, final Spreadsheet spreadsheet, final ExecutionYear executionYear) {
        if (event.isCancelled() && event.getSapRequestSet().isEmpty()) {
            return;
        }
        Row row = spreadsheet.addRow();        
        row.setCell("EventoID", event.getExternalId());
        row.setCell("Evento", event.getDescription().toString());
        row.setCell("Cancelado", event.isCancelled() ? "Sim" : "Não");
        row.setCell("Dívida original", event.getOriginalAmountToPay().getAmountAsString());
        row.setCell("Dívida actual", event.getAmountToPay().getAmountAsString());
        Degree degree = event.getDegree();
        row.setCell("Curso", degree.getPresentationName(executionYear));
        String ciclo = "";
        if (event.getDegree().isFirstCycle()) {
            ciclo = "1º ciclo";
        } else if (event.getDegree().isSecondCycle()) {
            ciclo = "2º ciclo";
        } else if (event.getDegree().isThirdCycle()) {
            ciclo = "3º ciclo";
        }
        row.setCell("Ciclo", ciclo);
        boolean isAlameda = degree.getCampus(executionYear).stream().anyMatch(c -> c.getName().contains("Alameda"));
        row.setCell("Campus", isAlameda ? "Alameda" : "Tagus");
        row.setCell("Regime", event.getStudentCurricularPlan().getRegistration().getRegimeType(executionYear).toString());
        row.setCell("istID", event.getPerson().getUsername());
        row.setCell("Nome", event.getPerson().getName());
        row.setCell("NIF", event.getPerson().getSocialSecurityNumber());
        row.setCell("Nacionalidade", event.getPerson().getCountry().getName());
    }

}
