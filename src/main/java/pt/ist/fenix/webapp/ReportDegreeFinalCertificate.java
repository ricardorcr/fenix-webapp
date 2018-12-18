package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;

import org.fenixedu.academic.domain.serviceRequests.documentRequests.DegreeFinalizationCertificateRequest;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.format.ISODateTimeFormat;

public class ReportDegreeFinalCertificate extends CustomTask {

    @Override
    public void runTask() throws Exception {
        final DateTime startDate = new LocalDate(2018,9,1).toDateTimeAtStartOfDay();
        final DateTime endDate = new DateTime(2019,12,31,23,59,59);
        Interval interval = new Interval(startDate, endDate);
        Spreadsheet spreadsheet = new Spreadsheet("Certidões Fim de Curso");
        
        Bennu.getInstance().getAcademicServiceRequestsSet().stream()
            .filter(DegreeFinalizationCertificateRequest.class::isInstance)
            .map(asr -> (DegreeFinalizationCertificateRequest)asr)
            .filter(fcr -> fcr.getDegreeType().isFirstCycle() || fcr.getDegreeType().isSecondCycle())
            .filter(fcr -> interval.contains(fcr.getCreationDate()))
            .forEach(fcr -> report(fcr, spreadsheet));
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("declaracoes_fim_curso.xls", baos.toByteArray());
    }

    private void report(DegreeFinalizationCertificateRequest fcr, Spreadsheet spreadsheet) {
        Row row = spreadsheet.addRow();
        row.setCell("Nº Aluno", fcr.getStudent().getNumber());
        row.setCell("Nome", fcr.getStudent().getName());
        row.setCell("Curso", fcr.getDegree().getName());
        row.setCell("Ciclo", fcr.getRequestedCycle() != null ? fcr.getRequestedCycle().toString() : " - ");
        row.setCell("Data pedido", fcr.getRequestDate().toString(ISODateTimeFormat.date()));
        row.setCell("Data conclusão", fcr.getRequestConclusionDate() != null ? fcr.getRequestConclusionDate().toString(ISODateTimeFormat.date()) : " - ");
        row.setCell("Estado", fcr.getActiveSituation() != null ? fcr.getActiveSituation().getAcademicServiceRequestSituationType().toString() : " - ");
        row.setCell("Nº pedido", fcr.getServiceRequestNumber());
    }

}
