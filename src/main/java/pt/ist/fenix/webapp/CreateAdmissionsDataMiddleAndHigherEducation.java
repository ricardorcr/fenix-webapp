package pt.ist.fenix.webapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessCost;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

public class CreateAdmissionsDataMiddleAndHigherEducation extends CustomTask {

    //prod /afs/ist.utl.pt/ciist/fenix/fenix015/ist/
    //local /home/rcro/DocumentsHDD/fenix/candidaturas/
    private static final Locale PT = new Locale("pt", "PT");
    private static final Locale EN = new Locale("en", "GB");
    private static final String FORM_DATA_FILENAME = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/middleAndHigherEducationFormData.json";
    private static final String DEGREES_DATA_FILENAME = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/cursos_2ciclo_2021_2022.csv";

    @Override
    public void runTask() throws Exception {
        final User responsible = User.findByUsername("ist24616");
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(admissionProcess -> admissionProcess.getTitle().getContent(PT).contains("Titulares"))
                .forEach(admissionProcess -> {
                    admissionProcess.getMemberSet().clear();
                    admissionProcess.getAdmissionProcessTargetSet().stream()
                            .forEach(apt -> apt.getJurySet().clear());
                    admissionProcess.getLogSet().forEach(log -> log.delete());
//                    admissionProcess.getAdmissionProcessCostSet().forEach(cost -> {
//                        cost.getApplicationSet().forEach(app -> {
//                            CustomEvent event = (CustomEvent) app.getEvent();
//                            if (event != null) {
//                                app.setEvent(null);
//                                //event.setCustomToAccount(null);
//                                event.cancel(responsible.getPerson(), "Dívida foi criada em resultado de testes na plataforma de candidaturas. Não de uma candidatura real.");
//                            }
//                        });
//                    });
                    admissionProcess.delete();
                });
        createMiddleAndHigherEducation();
    }

    private void createMiddleAndHigherEducation() throws Exception {
        final LocalizedString title = ls("Candidaturas Titulares de Cursos Superiores 2021/2022", "Holders of Middle Level and Higher Education Courses Applicants 2021/2022");
        final DateTime startDate = new DateTime(2021,07,05,0,0);
        final DateTime endDate = new DateTime(2021,07,23,23,59, 59);
        final LocalizedString tags = ls("Titulares Superiores 2021/2022", "Higher Education 2021/2022");

        final AdmissionProcess admissionProcess = AdmissionProcess.createAdmissionProcess(
                title,
                startDate,
                endDate,
                "https://tecnico.ulisboa.pt/pt/ensino/estudar-no-tecnico/candidaturas-e-inscricoes/titulares-de-curso-medio-e-superior/",
                tags,
                "admissions@tecnico.ulisboa.pt"
        );
        //admissionProcess.setComplaintDeadline(new DateTime().plusWeeks(5).plusDays(2));
        //admissionProcess.setResultsPublicationDate(new DateTime().plusWeeks(4).plusDays(3));
        admissionProcess.setOutcomeConfig(getHigherEducationOutcome().toString());
        setFormData(admissionProcess);
        setGradeConfig(admissionProcess);

        createTargets(admissionProcess);
        final JsonObject config = new JsonObject();
        config.add("description", ls("Emolumento de candidatura", "Application Fee").json());
        config.addProperty("productCode", "0031");
        config.addProperty("productDescription", "TAXAS DE MATRICULA");
        config.addProperty("accountId", "287762860391");

        final AdmissionProcessCost admissionProcessCost = admissionProcess.addAdmissionProcessCost(
                ls("Emolumento de candidatura", "Application fee"), new BigDecimal(100));
        admissionProcessCost.setConfig(config.toString());

//        final AdmissionProcessCost admissionProcessCostIST = admissionProcess.addAdmissionProcessCost(
//                ls("Emolumento de candidatura (para alunos ou ex-alunos IST)", "Application fee (for current or past IST students)"),
//                new BigDecimal(50));
//        admissionProcessCostIST.setConfig((config.toString()));
    }

    private void createTargets(AdmissionProcess admissionProcess) {
        try {
            final List<String> lines = Files.readAllLines(new File(DEGREES_DATA_FILENAME).toPath());
            lines.forEach(line -> {
                String[] split = line.split("\t");
                String degreeCode = split[3];
                final Degree degree = Degree.readBySigla(degreeCode);
                LocalizedString name = ls(degree.getPresentationNameI18N().getContent(PT),
                        degree.getPresentationNameI18N().getContent(EN));
                final AdmissionProcessTarget admissionProcessTarget = admissionProcess.createAdmissionProcessTarget(
                        name, Integer.valueOf(split[4]));
                String ingressionTypeCode = split[1];
                final IngressionType ingressionType = IngressionType.findIngressionTypeByCode(ingressionTypeCode).get();

                setOutcome(admissionProcessTarget, degree, ingressionType);
            });
        } catch (IOException e) {
            e.printStackTrace();
            taskLog("Error reading degrees file");
        }
    }

    private void setOutcome(AdmissionProcessTarget admissionProcessTarget, Degree degree, IngressionType ingressionType) {
        final RegistrationProtocol protocol = Bennu.getInstance().getRegistrationProtocolsSet().stream()
                .filter(p -> p.getCode().equals("NORMAL"))
                .findAny().orElse(null);

        final JsonObject outcome = new JsonObject();
        outcome.addProperty("degree", degree.getExternalId());
        outcome.addProperty("protocol", protocol.getExternalId());
        outcome.addProperty("ingressionType", ingressionType.getExternalId());
        outcome.addProperty("year", ExecutionYear.readCurrentExecutionYear().getNextExecutionYear().getExternalId());
        outcome.addProperty("cycleType", CycleType.SECOND_CYCLE.name());
        outcome.add("actionName", ls("Matrícular", "Enroll").json());

        admissionProcessTarget.setOutcomeConfig(outcome.toString());
    }

    private void setGradeConfig(AdmissionProcess admissionProcess) {
        JsonObject gradeConfig = new JsonObject();
        JsonArray components = new JsonArray();

        JsonObject a = new JsonObject();
        a.addProperty("name","a");
        a.add("description", ls("Afinidade", "Afinity").json());
        components.add(a);

        JsonObject b = new JsonObject();
        b.addProperty("name", "b");
        b.add("description", ls("Natureza", "Nature").json());
        components.add(b);

        JsonObject c = new JsonObject();
        c.addProperty("name", "c");
        c.add("description", ls("MFC", "MFC").json());
        c.addProperty("minForAdmission", "100");
        components.add(c);

        JsonObject d = new JsonObject();
        d.addProperty("name", "d");
        d.add("description", ls("Bónus", "Bonus").json());
        components.add(d);

        gradeConfig.add("components", components);
        gradeConfig.add("formula", JsonUtils.parse("{" +
                "\"add\": [" +
                "{ \"mul\": [80, \"a\"] }," +
                "{ \"mul\": [12, \"b\"] }," +
                "{ \"mul\": [0.3, \"c\"] }," +
                "{ \"mul\": [1, \"d\"] }" +
                "]" +
                "}"));
        gradeConfig.addProperty("minForAdmission", "100");
        //GrandConfig Mes {"components":[{"name":"a","description":{"pt-PT":"Afinidade","en-EN":"Afinity"},"weight":80},
        // {"name":"b","description":{"pt-PT":"Natureza","en-EN":"Nature"},"weight":12},
        // {"name":"c","description":{"pt-PT":"MFC","en-EN":"MFC"},"weight":0.3,"minForAdmission":100},
        // {"name":"d","description":{"pt-PT":"Bonus","en-EN":"Bonus"},"weight":1}],
        // "minForAdmission":100}

        admissionProcess.setGradeConfig(gradeConfig.toString());
    }

    private void setFormData(AdmissionProcess process) throws Exception{
        final JsonObject formData = JsonParser.parseString(
                new String(Files.readAllBytes(new File(FORM_DATA_FILENAME).toPath()))
        ).getAsJsonObject();
        process.setFormData(formData.toString());

    }

    private JsonObject getHigherEducationOutcome () {
        JsonObject outcomeConfig = new JsonObject();
        JsonObject type = new JsonObject();
        type.addProperty("name", "degree");
        type.add("title", ls("Curso", "Degree").json());
        type.add("answer", ls("Quero candidatar-me a um curso", "I want to apply to a higher education degree").json());
        outcomeConfig.add("type", type);
        return outcomeConfig;
    }

    private LocalizedString ls(final String pt, final String en) {
        return new LocalizedString(PT, pt).with(EN, en);
    }
}