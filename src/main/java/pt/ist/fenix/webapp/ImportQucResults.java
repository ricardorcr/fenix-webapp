package pt.ist.fenix.webapp;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionCourse;
import org.fenixedu.academic.domain.ExecutionDegree;
import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.Professorship;
import org.fenixedu.academic.domain.ShiftType;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.reports.GepReportFile;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.bennu.io.domain.GroupBasedFile;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ist.fenixedu.quc.domain.InquiryConnectionType;
import pt.ist.fenixedu.quc.domain.InquiryQuestion;
import pt.ist.fenixedu.quc.domain.InquiryResult;
import pt.ist.fenixedu.quc.domain.InquiryResultType;
import pt.ist.fenixedu.quc.domain.ResultClassification;
import pt.ist.fenixedu.quc.domain.ResultsImportationProcess;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ImportQucResults extends CustomTask {

    private static final int PARTITION_SIZE = 10000;

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        GroupBasedFile qucResults = FenixFramework.getDomainObject("3096843219145297");
        try (final InputStreamReader streamReader = new InputStreamReader(qucResults.getStream());){
            String stringResults = CharStreams.toString(streamReader);
            List<String> results = Splitter.on("\r\n").splitToList(stringResults);
            taskLog("#File lines: %s%n", results.size());

            DateTime resultDate = new DateTime(2025, 3, 18, 14, 15);
//            InquiryResult.importResults(stringResults, resultDate);
            importResults(stringResults, resultDate, results);

            Signal.emit(ResultsImportationProcess.QUC_RESULTS_IMPORTED,new DomainObjectEvent<>(null));
        }
    }

    public void importResults(String stringResults, DateTime resultDate, List<String> results) {

        List<List<String>> partition = new ArrayList<>(Lists.partition(results, PARTITION_SIZE));

        int index = 1;
        for (final List<String> resultPartition : partition) {
            taskLog("Importing %s of %s results%n", index * PARTITION_SIZE, results.size());
            ImportQucResults.Importer importer = new ImportQucResults.Importer(resultPartition.toArray(new String[0]), resultDate);
            Thread thread = new Thread(importer);
            try {
                thread.start();
                thread.join();
            } catch (InterruptedException e) {
                if (importer.exception != null) {
                    throw new Error(importer.exception);
                }
                throw new Error(e);
            }
            index++;
        }
    }

    private class Importer implements Runnable {

        private final String[] rows;
        private final DateTime resultDate;
        private Throwable exception;

        public Importer(final String[] rows, final DateTime resultDate) {
            this.rows = rows;
            this.resultDate = resultDate;
        }

        @Override
        public void run() {
            try {
                FenixFramework.atomic(() -> {
                    importRows(rows, resultDate);
                });
            }catch (Throwable t) {
                taskLog("Error while importing rows: %s%n", t.getMessage());
                exception = t;
                throw t;
            }
        }
    }

    private void importRows(String[] rows, DateTime resultDate) {
        for (String row : rows) {
            if (!Strings.isNullOrEmpty(row)) {
                String[] columns = row.split("\t");

                //TODO rever indices das colunas
                //columns[columns.length - 1] = columns[columns.length - 1].split("\r")[0];
                //meter aqui algumas validações
                //se vier com valor + classificação dá erro

                InquiryResult inquiryResult = new InquiryResult();
                inquiryResult.setResultDate(resultDate);

                setConnectionType(columns, inquiryResult);
                setClassification(columns, inquiryResult);
                setInquiryRelation(columns, inquiryResult);
                setExecutionSemester(columns, inquiryResult);
                setResultType(columns, inquiryResult);
                setValue(columns, inquiryResult);
            }
        }
    }

    private static void setValue(String[] columns, InquiryResult inquiryResult) {
        String value = columns[5] != null ? columns[5].replace(",", ".") : columns[5];
        String scaleValue = columns[6];
        inquiryResult.setValue(value);
        inquiryResult.setScaleValue(scaleValue);
    }

    private static void setConnectionType(String[] columns, InquiryResult inquiryResult) {
        String connectionTypeString = columns[10];
        if (Strings.isNullOrEmpty(connectionTypeString)) {
            throw new DomainException("connectionType: " + getPrintableColumns(columns));
        }
        InquiryConnectionType connectionType = InquiryConnectionType.valueOf(connectionTypeString.trim());
        inquiryResult.setConnectionType(connectionType);
    }

    private static void setResultType(String[] columns, InquiryResult inquiryResult) {
        String resultTypeString = columns[1];
        if (!Strings.isNullOrEmpty(resultTypeString)) {
            InquiryResultType inquiryResultType = InquiryResultType.valueOf(resultTypeString);
            if (inquiryResultType == null) {
                throw new DomainException("resultType: " + getPrintableColumns(columns));
            }
            inquiryResult.setResultType(inquiryResultType);
        }
    }

    private static void setClassification(String[] columns, InquiryResult inquiryResult) {
        String resultClassificationString = columns[4];
        if (!Strings.isNullOrEmpty(resultClassificationString)) {
            ResultClassification classification = ResultClassification.valueOf(resultClassificationString);
            if (classification == null) {
                throw new DomainException("classification: " + getPrintableColumns(columns));
            }
            inquiryResult.setResultClassification(classification);
        }
    }

    private static void setExecutionSemester(String[] columns, InquiryResult inquiryResult) {
        String executionPeriodCode = columns[3];
        ExecutionSemester executionSemester = getExecutionSemester(executionPeriodCode);
        if (executionSemester == null) {
            throw new DomainException("executionPeriod: " + getPrintableColumns(columns));
        }
        inquiryResult.setExecutionPeriod(executionSemester);
    }

    /**
     * Receives a unique code that identifies a ExecutionSemester and returns the domain object
     *
     * @param code - the semester plus the year
     * @return the correspondent ExecutionSemester
     */
    private static ExecutionSemester getExecutionSemester(String code) {
        String[] decodedParts = code.split(GepReportFile.CODE_SEPARATOR);
        return ExecutionSemester.readBySemesterAndExecutionYear(Integer.valueOf(decodedParts[0]), decodedParts[1]);
    }

    /**
     * Receives a unique code that identifies a ExecutionCourse and returns the domain object
     *
     * @param code - the execution course sigla plus the execution semester code
     * @return the correspondent ExecutionCourse
     */
    public static ExecutionCourse getExecutionCourse(String code) {
        String[] decodedParts = code.split(GepReportFile.CODE_SEPARATOR);
        return ExecutionCourse.readBySiglaAndExecutionPeriod(decodedParts[0], getExecutionSemester(decodedParts[1]
                + GepReportFile.CODE_SEPARATOR + decodedParts[2]));
    }

    /**
     * Receives a unique code that identifies a ExecutionDegree and returns the domain object
     *
     * @param code - the code of the degree curricular plan plus the execution year code
     * @return the correspondent ExecutionDegree
     */
    public static ExecutionDegree getExecutionDegree(String code) {
        String[] decodedParts = code.split(GepReportFile.CODE_SEPARATOR);
        DegreeCurricularPlan dcp = getDegreeCurricularPlan(decodedParts[0] + GepReportFile.CODE_SEPARATOR + decodedParts[1]);
        ExecutionYear executionYear = getExecutionYear(decodedParts[2]);
        return ExecutionDegree.getByDegreeCurricularPlanAndExecutionYear(dcp, executionYear);
    }

    /**
     * Receives a unique code that identifies a DegreeCurricularPlan and returns the domain object
     *
     * @param code - the name of the curricular plan plus the degree sigla
     * @return the correspondent DegreeCurricularPlan
     */
    private static DegreeCurricularPlan getDegreeCurricularPlan(String code) {
        String[] decodedParts = code.split(GepReportFile.CODE_SEPARATOR);
        return DegreeCurricularPlan.readByNameAndDegreeSigla(decodedParts[0], decodedParts[1]);
    }

    private static ExecutionYear getExecutionYear(String code) {
        return ExecutionYear.readExecutionYearByName(code);
    }

    /**
     * Receives a unique code that identifies a Professorship and returns the domain object
     *
     * @param code - the person username plus the execution course code
     * @return the correspondent Professorship
     */
    public static Professorship getProfessorship(String code) {
        String[] decodedParts = code.split(GepReportFile.CODE_SEPARATOR);
        ExecutionCourse executionCourse =
                getExecutionCourse(decodedParts[1] + GepReportFile.CODE_SEPARATOR + decodedParts[2]
                        + GepReportFile.CODE_SEPARATOR + decodedParts[3]);
        return executionCourse.getProfessorship(Person.findByUsername(decodedParts[0]));
    }

    /*
     * OID_EXECUTION_DEGREE RESULT_TYPE OID_EXECUTION_COURSE OID_EXECUTION_PERIOD RESULT_CLASSIFICATION VALUE_ SCALE_VALUE
     * OID_INQUIRY_QUESTION OID_PROFESSORSHIP SHIFT_TYPE CONNECTION_TYPE
     */
    private static void setInquiryRelation(String[] columns, InquiryResult inquiryResult) {
        String inquiryQuestionCode = columns[7];
        String executionCourseCode = columns[2];
        String executionDegreeCode = columns[0];
        String professorshipCode = columns[8];
        String shiftTypeString = columns[9];
        ExecutionCourse executionCourse =
                !Strings.isNullOrEmpty(executionCourseCode) ? getExecutionCourse(executionCourseCode) : null;
        ExecutionDegree executionDegree =
                !Strings.isNullOrEmpty(executionDegreeCode) ? getExecutionDegree(executionDegreeCode) : null;
        Professorship professorship =
                !Strings.isNullOrEmpty(professorshipCode) ? (Professorship) getProfessorship(professorshipCode) : null;
        ShiftType shiftType = !Strings.isNullOrEmpty(shiftTypeString) ? ShiftType.valueOf(shiftTypeString) : null;
        inquiryResult.setExecutionCourse(executionCourse);
        inquiryResult.setExecutionDegree(executionDegree);
        inquiryResult.setProfessorship(professorship);
        inquiryResult.setShiftType(shiftType);

        if (!(Strings.isNullOrEmpty(inquiryQuestionCode) && ResultClassification.GREY.equals(inquiryResult
                .getResultClassification()))) {
            InquiryQuestion inquiryQuestion = getInquiryQuestion(Long.valueOf(inquiryQuestionCode));
            if (inquiryQuestion == null) {
                throw new DomainException("não tem question: " + getPrintableColumns(columns));
            }
            inquiryResult.setInquiryQuestion(inquiryQuestion);
        }
    }

    public static InquiryQuestion getInquiryQuestion(Long inquiryQuestionCode) {
        for (InquiryQuestion inquiryQuestion : Bennu.getInstance().getInquiryQuestionsSet()) {
            if (inquiryQuestion.getCode().equals(inquiryQuestionCode)) {
                return inquiryQuestion;
            }
        }
        return null;
    }

    private static String getPrintableColumns(String[] columns) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String value : columns) {
            stringBuilder.append(value).append("\t");
        }
        return stringBuilder.toString();
    }

    private static class InquiryResultBean implements Serializable {

        private static final long serialVersionUID = 1L;
        private String value;
        private String scaleValue;
        private InquiryConnectionType connectionType;
        private InquiryResultType resultType;
        private ResultClassification resultClassification;
        private ExecutionSemester executionSemester;
        private ExecutionDegree executionDegree;
        private ExecutionCourse executionCourse;
        private InquiryQuestion inquiryQuestion;
        private Professorship professorship;
        private ShiftType shiftType;

        public InquiryResultBean(String[] row) {

            //TODO rever indices das colunas
            //columns[columns.length - 1] = columns[columns.length - 1].split("\r")[0];
            //meter aqui algumas validações
            //se vier com valor + classificação dá erro

            String executionDegreeCode = row[0];
            ExecutionDegree executionDegree =
                    !Strings.isNullOrEmpty(executionDegreeCode) ? InquiryResult.getExecutionDegree(executionDegreeCode) : null;
            setExecutionDegree(executionDegree);

            String resultTypeString = row[1];
            if (!Strings.isNullOrEmpty(resultTypeString)) {
                InquiryResultType inquiryResultType = InquiryResultType.valueOf(resultTypeString);
                if (inquiryResultType == null) {
                    throw new DomainException("resultType doesn't exists: " + getPrintableColumns(row));
                }
                setResultType(inquiryResultType);
            }

            String executionCourseCode = row[2];
            ExecutionCourse executionCourse =
                    !Strings.isNullOrEmpty(executionCourseCode) ? InquiryResult.getExecutionCourse(executionCourseCode) : null;
            setExecutionCourse(executionCourse);

            String executionPeriodCode = row[3];
            ExecutionSemester executionSemester = ImportQucResults.getExecutionSemester(executionPeriodCode);
            if (executionSemester == null) {
                throw new DomainException("executionPeriod resultType doesn't exists: " + getPrintableColumns(row));
            }
            setExecutionSemester(executionSemester);

            String resultClassificationString = row[4];
            if (!Strings.isNullOrEmpty(resultClassificationString)) {
                ResultClassification classification = ResultClassification.valueOf(resultClassificationString);
                if (classification == null) {
                    throw new DomainException("classification doesn't exists: : " + getPrintableColumns(row));
                }
                setResultClassification(classification);
            }

            String value = row[5] != null ? row[5].replace(",", ".") : row[5];
            String scaleValue = row[6];
            setValue(value);
            setScaleValue(scaleValue);

            String inquiryQuestionCode = row[7];
            if (!(Strings.isNullOrEmpty(inquiryQuestionCode) && ResultClassification.GREY.equals(getResultClassification()))) {
                InquiryQuestion inquiryQuestion = InquiryResult.getInquiryQuestion(Long.valueOf(inquiryQuestionCode));
                if (inquiryQuestion == null) {
                    throw new DomainException("não tem question: " + getPrintableColumns(row));
                }
                setInquiryQuestion(inquiryQuestion);
            }

            String professorshipCode = row[8];
            Professorship professorship =
                    !Strings.isNullOrEmpty(professorshipCode) ? InquiryResult.getProfessorship(professorshipCode) : null;
            setProfessorship(professorship);

            String shiftTypeString = row[9];
            ShiftType shiftType = !Strings.isNullOrEmpty(shiftTypeString) ? ShiftType.valueOf(shiftTypeString) : null;
            setShiftType(shiftType);

            String connectionTypeString = row[10];
            if (Strings.isNullOrEmpty(connectionTypeString)) {
                throw new DomainException("connectionType doesn't exists: " + getPrintableColumns(row));
            }
            InquiryConnectionType connectionType = InquiryConnectionType.valueOf(connectionTypeString);
            setConnectionType(connectionType);
        }

        private String getPrintableColumns(String[] columns) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String value : columns) {
                stringBuilder.append(value).append("\t");
            }
            return stringBuilder.toString();
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getScaleValue() {
            return scaleValue;
        }

        public void setScaleValue(String scaleValue) {
            this.scaleValue = scaleValue;
        }

        public InquiryConnectionType getConnectionType() {
            return connectionType;
        }

        public void setConnectionType(InquiryConnectionType connectionType) {
            this.connectionType = connectionType;
        }

        public InquiryResultType getResultType() {
            return resultType;
        }

        public void setResultType(InquiryResultType resultType) {
            this.resultType = resultType;
        }

        public ResultClassification getResultClassification() {
            return resultClassification;
        }

        public void setResultClassification(ResultClassification resultClassification) {
            this.resultClassification = resultClassification;
        }

        public ExecutionSemester getExecutionSemester() {
            return executionSemester;
        }

        public void setExecutionSemester(ExecutionSemester executionSemester) {
            this.executionSemester = executionSemester;
        }

        public ExecutionDegree getExecutionDegree() {
            return executionDegree;
        }

        public void setExecutionDegree(ExecutionDegree executionDegree) {
            this.executionDegree = executionDegree;
        }

        public ExecutionCourse getExecutionCourse() {
            return executionCourse;
        }

        public void setExecutionCourse(ExecutionCourse executionCourse) {
            this.executionCourse = executionCourse;
        }

        public InquiryQuestion getInquiryQuestion() {
            return inquiryQuestion;
        }

        public void setInquiryQuestion(InquiryQuestion inquiryQuestion) {
            this.inquiryQuestion = inquiryQuestion;
        }

        public Professorship getProfessorship() {
            return professorship;
        }

        public void setProfessorship(Professorship professorship) {
            this.professorship = professorship;
        }

        public ShiftType getShiftType() {
            return shiftType;
        }

        public void setShiftType(ShiftType shiftType) {
            this.shiftType = shiftType;
        }

    }
}