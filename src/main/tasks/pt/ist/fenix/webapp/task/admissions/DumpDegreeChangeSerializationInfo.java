package pt.ist.fenix.webapp.task.admissions;

import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.student.curriculum.ICurriculum;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.util.DynamicForm;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.connect.domain.Identity;
import pt.ist.fenixframework.FenixFramework;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Stream;

public class DumpDegreeChangeSerializationInfo extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        final Spreadsheet spreadsheet = new Spreadsheet("IST_Grades");

        final AdmissionProcess process = FenixFramework.getDomainObject("852907490541645");
        process.getAdmissionProcessTargetSet().stream()
                .flatMap(target -> target.getApplicationSet().stream())
                .filter(application -> application.getLockInstant() != null)
                .forEach(application -> {
                    final Identity identity = application.getAccount().getIdentity();
                    final User user = identity.getUser();
                    final Person person = user == null ? null : user.getPerson();
                    final Student student = person == null ? null : person.getStudent();

                    final RegistrationDataByExecutionYear data = student == null ? null : student.getRegistrationsSet().stream()
                            .filter(registration -> !registration.getDegree().isEmpty())
                            .flatMap(registration -> registration.getRegistrationDataByExecutionYearSet().stream())
                            .max(RegistrationDataByExecutionYear::compareTo)
                            .orElse(null);
                    final Registration registration = data == null ? null : data.getRegistration();
                    final long enrolledYears = registration == null ? 0l : registrationChain(registration).flatMap(r -> r.getRegistrationDataByExecutionYearSet().stream())
                            .map(rd -> rd.getExecutionYear())
                            .distinct()
                            .count();
                    final ICurriculum curriculum = registration == null ? null : registration.getCurriculum();
                    final BigDecimal finalGrade = registration == null ? null : curriculum.getRawGrade().getNumericValue()
                            .multiply(new BigDecimal(10))
                            .setScale(0, RoundingMode.HALF_EVEN);
                    final BigDecimal ects = registration == null ? null : curriculum.getSumEctsCredits();
                    final BigDecimal ma;
                    if (registration == null) {
                        ma = null;
                    } else {
                        final BigDecimal divisor = new BigDecimal(60).multiply(new BigDecimal(enrolledYears));
                         ma = finalGrade
                                .multiply(ects)
                                .divide(divisor, 0, RoundingMode.HALF_EVEN);
                    }
                    final JsonObject form = application.getAdmissionProcessTarget().getAdmissionProcess().getFormDataJson();
                    final DynamicForm dynamicForm = new DynamicForm(form).withData(application.getDataObject().getAsJsonObject("formData"));
                    final DynamicForm.Quantity highschool_average = dynamicForm.get("highschool_average");
                    final DynamicForm.Quantity first_ingression_grade = dynamicForm.get("first_ingression_grade");
                    final DynamicForm.Quantity second_ingression_grade = dynamicForm.get("second_ingression_grade");

                    final BigDecimal p1 = first_ingression_grade == null || first_ingression_grade.value() == null ? null : first_ingression_grade.value();
                    final BigDecimal p2 = second_ingression_grade == null || second_ingression_grade.value() == null ? null : second_ingression_grade.value();
                    final BigDecimal pi = p1 == null && p2 == null ? null : p1 == null ? p2 : p2 == null ? p1 : p1.add(p2).divide(new BigDecimal(2), 1, RoundingMode.HALF_EVEN);

                    final Spreadsheet.Row row = spreadsheet.addRow();
                    row.setCell("application", application.getExternalId());
                    row.setCell("isISTStudent", Boolean.toString(registration != null));
                    row.setCell("username", registration == null ? "" : user.getUsername());
                    row.setCell("lastRegistration", registration == null ? "" : registration.getDegree().getSigla());
                    row.setCell("lastRegistrationState", registration == null ? "" : registration.getLastState().getStateType().getDescription());
                    row.setCell("enrolledYears", registration == null ? "" : Long.toString(enrolledYears));
                    row.setCell("weightedGrade", registration == null ? "" : finalGrade.toPlainString());
                    row.setCell("ects", registration == null ? "" : ects.toPlainString());
                    row.setCell("highschool_average", highschool_average == null ? "" : highschool_average.value().toPlainString());
                    row.setCell("first_ingression_grade", first_ingression_grade == null || first_ingression_grade.value() == null
                            ? "" : first_ingression_grade.value().toPlainString());
                    row.setCell("second_ingression_grade", second_ingression_grade == null || second_ingression_grade.value() == null
                            ? "" : second_ingression_grade.value().toPlainString());
                    row.setCell("MS", highschool_average == null ? "" : highschool_average.value().toPlainString());
                    row.setCell("PI", pi == null ? "" : pi.toPlainString());
                    row.setCell("MA", registration == null ? "" : ma.toPlainString());
                });

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        spreadsheet.exportToXLSSheet(stream);
        output("ist_grades.xlsx", stream.toByteArray());
    }

    private Stream<Registration> registrationChain(final Registration registration) {
        final Stream<Registration> stream = Stream.of(registration);
        final Registration source = registration.getSourceRegistration();
        return source == null || source.getSourceRegistration() == registration ? stream
                : Stream.concat(stream, registrationChain(source));
    }

}