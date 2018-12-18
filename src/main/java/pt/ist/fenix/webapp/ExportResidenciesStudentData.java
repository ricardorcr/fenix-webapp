package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.GradeScale;
import org.fenixedu.academic.domain.StudentCurricularPlan;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.Spreadsheet.Row;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic.TxMode;

public class ExportResidenciesStudentData extends CustomTask {

    final Integer[] RDP_STUDENTS =
            new Integer[] { 81821,83421,83422,83452,83651,83680,83697,83702,83720,83722,83723,83727,83736,83812,83844,84021,84106,
                    84218,84416,84417,84508,84704,84727,84766,86254,86583,86604,86606,86613,86616,86623,86632,86637,86653,86654,
                    86658,86664,86665,86667,86685,86760,87327,87336,87339,87343,87346,87348,87353,87354,87357,87358,89419,89423,
                    89471,89598,89625,89630,89636,89645,89650,89652,89653,89656,89658,89668,89674,89688,89691,89697,89698,89707,
                    89708,89709,89815,90233,90311,90369,90413,90421,91004,91676,92620,92650,92654,92674,92679,92680,92684,92698,
                    92699,92725,92730,92799,92836,93023,93322,93349,93365,93378,93386,93401,93402,93410,93412,93560,93797,93799,
                    94205,94288 };

    final Integer[] RRR_STUDENTS =
            new Integer[] { 84583,84607,84629,84708,84717,84729,84733,84750,84764,84773,85252,87548,87557,87623,87641,87642,87691,
                    90656,90662,90664,90666,90668,90681,90685,90686,90734,90758,90765,93591,93628,93647,93650,93662,93665,93671,
                    93677,93678,93688,93698,93699,93706,93707,93754,93992,94237 };

    private ExecutionSemester SEMESTER1 = null;
    private ExecutionSemester SEMESTER2 = null;

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        SEMESTER1 = ExecutionSemester.readBySemesterAndExecutionYear(1, "2019/2020");
        SEMESTER2 = ExecutionSemester.readBySemesterAndExecutionYear(2, "2018/2019");

        ByteArrayOutputStream rdpFileOS = new ByteArrayOutputStream();
        Spreadsheet rdpSpreadsheet = createSpreadsheet();
        for (Integer element : RDP_STUDENTS) {
            Student student = Student.readStudentByNumber(element);
            addInformation(rdpSpreadsheet, student);
        }
        rdpSpreadsheet.exportToXLSSheet(rdpFileOS);
        output(getFileName("Alunos_RDP_"), rdpFileOS.toByteArray());

        ByteArrayOutputStream rrrFileOS = new ByteArrayOutputStream();
        Spreadsheet rrrSpreadsheet = createSpreadsheet();
        for (Integer element : RRR_STUDENTS) {
            Student student = Student.readStudentByNumber(element);
            addInformation(rrrSpreadsheet, student);
        }
        rrrSpreadsheet.exportToXLSSheet(rrrFileOS);
        output(getFileName("Alunos_RRR_"), rrrFileOS.toByteArray());
    }

    private String getFileName(String startString) {
        String fileNameString = startString + SEMESTER1.getName() + "_" + SEMESTER1.getExecutionYear().getName()
                + "_" + SEMESTER2.getName() + "_" + SEMESTER2.getExecutionYear().getName()
                + "_" + new DateTime().toString("dd_MM_yyyy") + ".xls";
        return fileNameString.replace("/", "-");
    }

    private Set<Enrolment> getEnrolments(final Student student, final ExecutionSemester semester) {
        StudentCurricularPlan scp = getStudentCurricularPlan(student, semester);

        return scp.getAllCurriculumLines().stream().filter(line -> line.getExecutionPeriod() == semester)
                .filter(line -> line.isEnrolment()).filter(line -> !line.isDismissal()).map(line -> (Enrolment) line)
                .collect(Collectors.toSet());
    }

    private double getApprovedECTS(final Set<Enrolment> enrolments) {
        return enrolments.stream().filter(e -> e.isApproved()).mapToDouble(e -> e.getEctsCreditsForCurriculum().doubleValue())
                .sum();
    }

    private StudentCurricularPlan getStudentCurricularPlan(final Student student, final ExecutionSemester semester) {
        List<Registration> registrations = student.getActiveRegistrationsIn(semester);
        if (registrations.isEmpty()) {
            return null;
        }

        if (registrations.size() != 1) {
            //throw new DomainException("student.has.more.than.one.active.registration", null);
        }

        Registration registration = registrations.iterator().next();
        final StudentCurricularPlan studentCurricularPlan = registration.getLastStudentCurricularPlan();
        if (!studentCurricularPlan.isBolonhaDegree()) {
            throw new DomainException("student.curricular.plan.is.not.bolonha", "");
        }

        return studentCurricularPlan;
    }

    private Spreadsheet createSpreadsheet() {
        final Spreadsheet spreadsheet = new Spreadsheet("students");

        spreadsheet.setHeaders(new String[] { "istID", "Num Aluno", "Nome", "ECTS 1sem 2019/20", "ECTS 2sem 2018/19", "Total"});

        return spreadsheet;
    }

    private void addInformation(final Spreadsheet spreadsheet, final Student student) {
        Set<Enrolment> enrolmentsSemester1 = getEnrolments(student, SEMESTER1);
        Set<Enrolment> enrolmentsSemester2 = getEnrolments(student, SEMESTER2);

        final Row row = spreadsheet.addRow();
        row.setCell(student.getPerson().getUsername());
        row.setCell(student.getNumber());
        row.setCell(student.getPerson().getName());

        double approvedECTS1 = getApprovedECTS(enrolmentsSemester1);
        row.setCell(approvedECTS1);
        double approvedECTS2 = getApprovedECTS(enrolmentsSemester2);
        row.setCell(approvedECTS2);
        row.setCell(approvedECTS1+approvedECTS2);
    }
}