package pt.ist.dot.scripts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Function;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.LocalDate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pt.ist.sap.client.SapStaff;
import pt.ist.sap.client.SapStructure;
import pt.ist.sap.client.Util;

public class ListProfessionalData extends CustomTask {

    @Override
    public void runTask() throws Exception {
        report("1018", "1801", "1802", "1803");
    }

    private void report(final String... institutions) throws IOException {
        final Spreadsheet sheet = new Spreadsheet("report");
        for (final String institution : institutions) {
            final Row row = sheet.addRow();
            row.setCell("institution", institution);
        }

        final SapStaff sapProfessionalData = new SapStaff();
        Spreadsheet mid = export("situations", (i) -> sapProfessionalData.listProfessionalSituation(i), sheet, institutions);
        mid = export("categorias", (i) -> sapProfessionalData.listProfessionalCategory(i), mid, institutions);
        mid = export("regimes", (i) -> sapProfessionalData.listRegimes(i), mid, institutions);
        mid = export("sne", (i) -> sapProfessionalData.listServiceExcemptions(i), mid, institutions);
        mid = export("situacaoDaPessoa", (i) -> sapProfessionalData.listPersonProfessionalInformation(i), mid, institutions);
        mid = export("sabaticas", (i) -> sapProfessionalData.listPersonSabaticals(i), mid, institutions);

      //  final SapStructure sapStructure = new SapStructure();
       // mid = export("people", (i) -> sapStructure.listPeople(i), mid, institutions);

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        sheet.exportToXLSSheet(stream);
        String filename = "professionalData_"+new LocalDate().toString("YYYY_MM_dd")+".xls";
        output(filename, stream.toByteArray());
    }
  
  
    private Spreadsheet export(final String reportName, final Function<JsonObject, JsonArray> f, final Spreadsheet s,
            final String... institutions) throws IOException {
        final Spreadsheet sheet = s.addSpreadsheet(reportName);
        for (final String institution : institutions) {
            final JsonObject input = Util.toJson("institution", institution);
            final JsonArray out = f.apply(input);
            for (final JsonElement je : out) {
                final JsonObject jo = je.getAsJsonObject();
                final Row row = sheet.addRow();
                row.setCell("institution", institution);
                jo.entrySet().forEach(e -> {
                    row.setCell(e.getKey(), e.getValue().getAsString());
                });
            }
        }
        return sheet;
    }

}