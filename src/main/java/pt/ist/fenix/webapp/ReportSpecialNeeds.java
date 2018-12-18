package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class ReportSpecialNeeds extends CustomTask {

    @Override
    public void runTask() throws Exception {
//        final Spreadsheet spreadsheet = new Spreadsheet("Necessidades Educativas Especiais");
//        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
//            .filter(ap -> ap.getTitle().toString().contains("Nacional"))
//            .flatMap(ap -> ap.getApplicationSet().stream())
//            .forEach(app -> report(app, spreadsheet));
//        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        spreadsheet.exportToXLSSheet(baos);
//        output("Alunos_requereram_assistencia_necessidades_especiais.xls", baos.toByteArray());
//    }
//
//    private void report(Application app, Spreadsheet spreadsheet) {
//        String candidacyId = app.get("candidacyId");
//        if (!Strings.isNullOrEmpty(candidacyId)) {
//            StudentCandidacy candidacy = FenixFramework.getDomainObject(candidacyId);
//            if (candidacy.getRegistration() != null && candidacy.getInterestedInSpecialNeedsInformation() != null
//                    && candidacy.getInterestedInSpecialNeedsInformation()) {
//                Row row = spreadsheet.addRow();
//                row.setCell("Nº aluno", candidacy.getRegistration().getNumber());
//                row.setCell("Nome", candidacy.getPerson().getName());
//                row.setCell("Curso", candidacy.getRegistration().getDegreeName());
//                row.setCell("Telemóvel", candidacy.getPerson().getDefaultMobilePhoneNumber());
//                row.setCell("Email", candidacy.getPerson().getDefaultEmailAddressValue());
//            }
//        }
    }
}
