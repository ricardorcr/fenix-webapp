package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import pt.ist.fenixedu.contracts.domain.Employee;
import pt.ist.fenixedu.contracts.domain.accessControl.ActiveResearchers;

import java.io.ByteArrayOutputStream;

public class ListIndianPeople extends CustomTask {

    Country india = null;
    ActiveResearchers activeResearchers = null;
    Spreadsheet spreadsheet = new Spreadsheet("Indianos no IST");

    @Override
    public void runTask() throws Exception {
        india = Country.readByThreeLetterCode("IND");
        activeResearchers = new ActiveResearchers();
        Bennu.getInstance().getUserSet().stream()
                .filter(this::isIndian)
                .filter(this::isToReport)
                .forEach(this::report);

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("indianos_no_ist.xls", stream.toByteArray());
    }

    private void report(final User user) {
        Student student = user.getPerson().getStudent();
        if (student != null) {
            student.getRegistrationsSet().stream()
                    .forEach(r -> {
                        final Spreadsheet.Row row = spreadsheet.addRow();
                        row.setCell("Username", user.getUsername());
                        row.setCell("Nome", user.getPerson().getName());
                        row.setCell("Email", user.getPerson().getEmailForSendingEmails());
                        row.setCell("Categoria", r.getDegree().getDegreeType().getName().getContent());
                        row.setCell("Curso/Unidade", r.getDegreeCurricularPlanName());
                        row.setCell("Activo", r.isActive() ? "Sim" : "Não");
                    });
        } else if (activeResearchers.isMember(user)) {
            final Spreadsheet.Row row = spreadsheet.addRow();
            Employee employee = user.getPerson().getEmployee();
            row.setCell("Username", user.getUsername());
            row.setCell("Nome", user.getPerson().getName());
            row.setCell("Email", user.getPerson().getEmailForSendingEmails());
            row.setCell("Categoria", "Investigador");
            row.setCell("Curso/Unidade", employee.getCurrentWorkingPlace() != null ? employee.getCurrentWorkingPlace().getName() : "");
            row.setCell("Activo", "Sim");
        }
    }

    private boolean isToReport(final User user) {
        Student student = user.getPerson().getStudent();
        if (student != null) {
            return true;
        } else if (activeResearchers.isMember(user)) {
            return true;
        }
        return false;
    }

    private boolean isIndian(final User user) {
        return user.getPerson() != null &&
                (user.getPerson().getCountry() == india || user.getPerson().getCountryOfBirth() == india);
    }
}
