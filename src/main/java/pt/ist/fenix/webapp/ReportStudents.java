package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.PhdProgram;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationRegimeType;
import org.fenixedu.academic.domain.student.registrationStates.RegistrationStateType;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

public class ReportStudents extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final ExecutionYear executionYear = ExecutionYear.readExecutionYearByName("2019/2020");
        final Spreadsheet spreadsheet = new Spreadsheet("Alunos 2019-2020");
        
        Bennu.getInstance().getRegistrationsSet().stream()
            .filter(r -> r.hasAnyActiveState(executionYear))
            .filter(r -> {
                if (r.getDegree().isEmpty()) {
                   return r.hasAnyStandaloneEnrolmentsIn(executionYear); 
                } else {
                    return r.hasAnyEnrolmentsIn(executionYear) || r.hasStateType(executionYear, RegistrationStateType.MOBILITY);
                }
            })
            .forEach(r -> report(r, executionYear, spreadsheet));
        
        executionYear.getPhdIndividualProgramProcessesSet().stream()
            .forEach(p -> report(p, executionYear, spreadsheet));
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("Alunos_2019_2020.xls", baos.toByteArray());
    }

    private void report(final Registration registration, final ExecutionYear executionYear, final Spreadsheet spreadsheet) {
        Row row = spreadsheet.addRow();              
        Degree degree = registration.getDegree();
        row.setCell("istID", registration.getPerson().getUsername());
        row.setCell("Nome", registration.getPerson().getName());
        row.setCell("NIF", registration.getPerson().getSocialSecurityNumber());
        Country country = registration.getPerson().getCountry();
        row.setCell("Nacionalidade", country != null ? country.getName() : "Sem país");
        row.setCell("Curso", degree.getPresentationName(executionYear));
        String ciclo = "";
        if (degree.isFirstCycle()) {
            ciclo = "1º ciclo";
        } else if (degree.isSecondCycle()) {
            ciclo = "2º ciclo";
        } else if (degree.isThirdCycle()) {
            ciclo = "3º ciclo";
        }
        row.setCell("Ciclo", ciclo);
        boolean isAlameda = degree.getCampus(executionYear).stream().anyMatch(c -> c.getName().contains("Alameda"));
        row.setCell("Campus", isAlameda ? "Alameda" : "Tagus");
        row.setCell("Regime", registration.getRegimeType(executionYear).toString());
        row.setCell("Em mobilidade", registration.hasStateType(executionYear, RegistrationStateType.MOBILITY) ? "Sim" : "Não");
        row.setCell("Protocolo", registration.getRegistrationProtocol().getDescription().getContent());
        row.setCell("Ingresso", registration.getIngressionType() != null ? registration.getIngressionType().getLocalizedName() : "");
    }
    
    private void report(final PhdIndividualProgramProcess program, final ExecutionYear executionYear, final Spreadsheet spreadsheet) {
        Row row = spreadsheet.addRow();        
        PhdProgram phdProgram = program.getPhdProgram();
        row.setCell("istID", program.getPerson().getUsername());
        row.setCell("Nome", program.getPerson().getName());
        row.setCell("NIF", program.getPerson().getSocialSecurityNumber());
        Country country = program.getPerson().getCountry();
        row.setCell("Nacionalidade", country != null ? country.getName() : "Sem país");
        row.setCell("Curso", phdProgram.getPresentationName(executionYear));
        row.setCell("Ciclo", "3º ciclo");
        boolean isAlameda = phdProgram.getDegree().getCampus(executionYear).stream().anyMatch(c -> c.getName().contains("Alameda"));
        row.setCell("Campus", isAlameda ? "Alameda" : "Tagus");
        row.setCell("Regime", RegistrationRegimeType.FULL_TIME.toString());
        row.setCell("Em mobilidade", "");
        row.setCell("Protocolo", "");
        row.setCell("Ingresso", "");
    }
}
