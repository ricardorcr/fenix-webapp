package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacyProcess.mobility.MobilityApplicationProcess;
import org.fenixedu.academic.domain.candidacyProcess.mobility.MobilityQuota;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

public class ExportMobilityProcessData extends ReadCustomTask {

    final private static Locale EN = new Locale("en", "GB");

    @Override
    public void runTask() throws Exception {
        ExecutionYear nextYear = ExecutionYear.readCurrentExecutionYear().getNextExecutionYear();
        final MobilityApplicationProcess applicationProcess = MobilityApplicationProcess.getCandidacyProcessByExecutionInterval(MobilityApplicationProcess.class, nextYear);

        final Spreadsheet spreadsheet = new Spreadsheet("Dados Processos Mobilidade");
        applicationProcess.getApplicationPeriod().getMobilityQuotasSet().stream()
                .forEach(mq -> report(mq, spreadsheet));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(baos);
        output("dados_processo_mobilidade_20_21.xls", baos.toByteArray());
    }

    private void report(final MobilityQuota mobilityQuota, Spreadsheet spreadsheet) {
        final Row row = spreadsheet.addRow();
        row.setCell("Protocol", mobilityQuota.getMobilityAgreement().getMobilityProgram().getRegistrationProtocol().getCode());
        row.setCell("University", mobilityQuota.getMobilityAgreement().getUniversityUnit().getNameI18n().getContent(EN));
        row.setCell("Degree", mobilityQuota.getDegree().getSigla());
        row.setCell("Slots", mobilityQuota.getNumberOfOpenings());
    }
}
