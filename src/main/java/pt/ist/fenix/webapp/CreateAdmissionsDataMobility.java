package pt.ist.fenix.webapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.candidacyProcess.mobility.MobilityApplicationProcess;
import org.fenixedu.academic.domain.candidacyProcess.mobility.MobilityQuota;
import org.fenixedu.academic.domain.organizationalStructure.UniversityUnit;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CreateAdmissionsDataMobility extends CustomTask {

    //prod /afs/ist.utl.pt/ciist/fenix/fenix015/ist/
    //local /home/rcro/DocumentsHDD/fenix/candidaturas/
    private static final Locale PT = new Locale("pt", "PT");
    private static final Locale EN = new Locale("en", "GB");
    private static final String EUROPE_ERASMUS_DATA_FILENAME = "/home/rcro/DocumentsHDD/fenix/candidaturas/cursos_mobilidade_europe_erasmus_1s_2020_2021.csv";
    private static final String OUTSIDE_EUROPE_DATA_FILENAME = "/home/rcro/DocumentsHDD/fenix/candidaturas/cursos_mobilidade_outside_europe_1s_2020_2021.csv";
    private static final String DOUBLE_DEGREES_DATA_FILENAME = "/home/rcro/DocumentsHDD/fenix/candidaturas/cursos_mobilidade_double_degrees_1s_2020_2021.csv";
    private static final String FORM_DATA_FILENAME = "/home/rcro/DocumentsHDD/fenix/candidaturas/mobilityFormData.json";
    private static final String OUTCOME_FORM_DATA_FILENAME = "/home/rcro/DocumentsHDD/fenix/candidaturas/mobilityOutcomeFormData.json";

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(admissionProcess -> {
                    final DateTime startDate = admissionProcess.getStartApplicationSubmissionPeriod();
                    return startDate.getDayOfMonth() == 21 && startDate.getMonthOfYear() == 5;
                })
                .forEach(admissionProcess -> {
                    admissionProcess.getMemberSet().clear();
                    admissionProcess.getAdmissionProcessTargetSet().stream()
                            .forEach(apt -> apt.getJurySet().clear());
                    admissionProcess.getLogSet().forEach(log -> log.delete());
//                    admissionProcess.getAdmissionProcessTargetSet().stream()
//                            .flatMap(apt -> apt.getLogSet().stream())
//                            .forEach(log -> log.delete());
                    admissionProcess.delete();
                });


        processTargetFile(EUROPE_ERASMUS_DATA_FILENAME, FORM_DATA_FILENAME, "erasmus@tecnico.ulisboa.pt", false);
        processTargetFile(OUTSIDE_EUROPE_DATA_FILENAME, FORM_DATA_FILENAME, "outsideeurope@tecnico.ulisboa.pt", false);
        processTargetFile(DOUBLE_DEGREES_DATA_FILENAME, FORM_DATA_FILENAME, "nmci@tecnico.ulisboa.pt", true);
    }

    private void processTargetFile(final String targetPath, final String formPath, final String email, final boolean isDoubleDegree) throws IOException {
        Map<RegistrationProtocol, Set<UniversityInfo>> processTargets = new HashMap<>();
        final List<String> lineData = Files.readAllLines(new File(targetPath).toPath());
        for (String line : lineData) {
            String[] split = line.split("\t");
            Integer slots = Integer.valueOf(split[4]);
            String protocolCode = split[0];
            final RegistrationProtocol protocol = Bennu.getInstance().getRegistrationProtocolsSet().stream()
                    .filter(p -> p.getCode().equals(protocolCode))
                    .findAny().orElse(null);
            if (protocol == null) {
                taskLog("%s %s%n", protocolCode, targetPath);
            }
            final Set<UniversityInfo> universityInfos = processTargets.computeIfAbsent(protocol, (t) -> new HashSet<UniversityInfo>());
            String ingressionTypeCode = split[1];
            final IngressionType ingressionType = IngressionType.findIngressionTypeByCode(ingressionTypeCode).get();
            String universityName = split[2];
            String degreeCode = split[3];
            final UniversityInfo universityInfo = new UniversityInfo(universityName, slots, ingressionType, degreeCode);
            universityInfos.add(universityInfo);
        }

        processTargets.forEach((k,v) -> {
            createMobility(k,v, email, formPath, isDoubleDegree);
        });
    }

    private void createMobility(final RegistrationProtocol protocol, final Set<UniversityInfo> targets, final String email,
                                final String formPath, boolean isDoubleDegree) {
        final LocalizedString title = ls(protocol.getDescription().getContent(PT), protocol.getDescription().getContent(EN));
        final DateTime startDate = new DateTime(2021, 05, 21, 0, 0);
        final DateTime endDate = new DateTime(2021, 06, 20, 23, 59, 59);

        final LocalizedString tags = ls("Mobilidade 2021/2022", "Mobility 2021/2022");
        final String informationUrl = "";

        createMobility(title, startDate, endDate, tags, informationUrl, email, true, protocol, targets, formPath, isDoubleDegree);
    }

    private void createMobility(final LocalizedString title, final DateTime startDate, final DateTime endDate, final LocalizedString tags,
                                final String informationUrl, final String email, final boolean hasAdmissionGranted,
                                final RegistrationProtocol protocol, final Set<UniversityInfo> targets, final String formPath, boolean isDoubleDegree) {
        final AdmissionProcess admissionProcess = AdmissionProcess.createAdmissionProcess(
                title,
                startDate,
                endDate,
                informationUrl,
                tags,
                email
        );
        //admissionProcess.setComplaintDeadline(new DateTime().plusWeeks(5).plusDays(2));
        //admissionProcess.setResultsPublicationDate(new DateTime().plusWeeks(4).plusDays(3));
        admissionProcess.setStartOutcomePeriod(startDate);
        admissionProcess.setEndOutcomePeriod(endDate);
        if (isDoubleDegree) {
            admissionProcess.setOutcomeConfig(getHigherEducationOutcomeDoubleDegree().toString());
        } else {
            admissionProcess.setOutcomeConfig(getHigherEducationOutcome().toString());
        }
        admissionProcess.setHasAdmissionGranted(hasAdmissionGranted);
        setFormData(admissionProcess, formPath);
        createTargets(admissionProcess, protocol, targets);
    }

    private void createTargets(final AdmissionProcess admissionProcess, final RegistrationProtocol protocol, final Set<UniversityInfo> targets) {
        targets.stream()
                .forEach(universityInfo -> {
                    final IngressionType ingressionType = universityInfo.getIngressionType();
                    final Degree degree = Degree.readBySigla(universityInfo.getDegreeCode());
                    UniversityUnit universityUnit = getUniversityUnit(universityInfo.getUniversityName());
                    if (universityUnit == null) {
                        taskLog("#%s%n", universityInfo.getUniversityName());
                    }
                    String ptName = universityUnit.getNameI18n().getContent(PT);
                    String enName = universityUnit.getNameI18n().getContent(EN);
                    ptName = ptName != null ? ptName : enName;
                    enName = enName != null ? enName : ptName;
                    if (degree == null) {
                        taskLog("Não encontrei %s%n",universityInfo.getDegreeCode());
                    }
                    LocalizedString name = ls(
                            degree.getPresentationNameI18N().getContent(PT) + " (" + ptName + ")",
                            degree.getPresentationNameI18N().getContent(EN) + " (" + enName + ")"
                    );
                    final AdmissionProcessTarget admissionProcessTarget = admissionProcess.createAdmissionProcessTarget(
                            name, universityInfo.getSlots());

                    setOutcome(admissionProcessTarget, degree, protocol, ingressionType);
                    JsonArray tags = new JsonArray();
                    JsonObject tag = new JsonObject();
                    tag.addProperty("name", universityUnit.getExternalId());
                    tag.add("title", universityUnit.getNameI18n().json());
                    tags.add(tag);
                    admissionProcessTarget.setTagsConfig(tags.toString());
                });
    }

    private UniversityUnit getUniversityUnit(String universityNameEN) {
        ExecutionYear nextYear = ExecutionYear.readCurrentExecutionYear().getNextExecutionYear();
        final MobilityApplicationProcess applicationProcess = MobilityApplicationProcess.getCandidacyProcessByExecutionInterval(MobilityApplicationProcess.class, nextYear);
        final MobilityQuota mobilityQuota = applicationProcess.getApplicationPeriod().getMobilityQuotasSet().stream()
                .filter(mq -> {
                    String content = mq.getMobilityAgreement().getUniversityUnit().getNameI18n().getContent(EN);
                    if (content == null) {
                        content = mq.getMobilityAgreement().getUniversityUnit().getNameI18n().getContent();
                    }
                    return content.replace("\u00a0"," ").replaceAll("  ", " ").trim().equals(universityNameEN);
                })
                .findAny().orElse(null);
        if (mobilityQuota != null) {
            return mobilityQuota.getMobilityAgreement().getUniversityUnit();
        } else {
            return null;
        }
    }

    private void setOutcome(AdmissionProcessTarget admissionProcessTarget, Degree degree, RegistrationProtocol protocol, IngressionType ingressionType) {
        final JsonObject outcome = new JsonObject();
        outcome.addProperty("degree", degree.getExternalId());
        outcome.addProperty("protocol", protocol.getExternalId());
        outcome.addProperty("ingressionType", ingressionType.getExternalId());
        outcome.addProperty("year", ExecutionYear.readCurrentExecutionYear().getNextExecutionYear().getExternalId());
        outcome.add("actionName", ls("Matrícular", "Enroll").json());

        admissionProcessTarget.setOutcomeConfig(outcome.toString());
    }

    private void setFormData(final AdmissionProcess process, final String formPath) {
        final JsonObject formData = readJson(formPath);
        process.setFormData(formData.toString());
    }

    private JsonObject getHigherEducationOutcome() {
        JsonObject outcomeConfig = new JsonObject();
        JsonObject type = new JsonObject();
        type.addProperty("name", "mobilityInbound");
        type.add("title", ls("Mobilidade In", "Mobility Inbound").json());
        type.add("answer", ls("Quero ingressar via protocolo de mobilidade (Erasmus, OutsideEurope, etc.)",
                "I want to enrol via mobility program (Erasmus, OutsideEurope, etc.)").json());
        outcomeConfig.add("type", type);
        final JsonObject formData = readJson(OUTCOME_FORM_DATA_FILENAME);
        outcomeConfig.add("formData", formData);
        return outcomeConfig;
    }

    private JsonObject getHigherEducationOutcomeDoubleDegree() {
        JsonObject outcomeConfig = new JsonObject();
        JsonObject type = new JsonObject();
        type.addProperty("name", "mobilityInboundDoubleDegree");
        type.add("title", ls("Mobilidade In Duplo Grau", "Mobility Inbound Double Degree").json());
        type.add("answer", ls("Quero ingressar via protocolo de mobilidade de Duplo Grau (Innoenergy, Erasmus Mundus, etc.)",
                "I want to enrol via mobility program - Double Degree (Innoenergy, Erasmus Mundus, etc.)").json());
        outcomeConfig.add("type", type);
        final JsonObject formData = readJson(OUTCOME_FORM_DATA_FILENAME);
        outcomeConfig.add("formData", formData);
        return outcomeConfig;
    }

    private JsonObject readJson(final String filename) {
        try {
            return new JsonParser().parse(new String(Files.readAllBytes(new File(filename).toPath()))).getAsJsonObject();
        } catch (final IOException e) {
            throw new Error(e);
        }
    }

    private LocalizedString ls(final String pt, final String en) {
        return new LocalizedString(PT, pt).with(EN, en);
    }

    private class UniversityInfo {

        IngressionType ingressionType;
        String universityName;
        Integer slots;
        String degreeCode;

        public UniversityInfo(final String universityName, final Integer slots, final IngressionType ingressionType, final String degreeCode) {
            this.universityName = universityName;
            this.slots = slots;
            this.ingressionType = ingressionType;
            this.degreeCode = degreeCode;
        }

        public String getUniversityName() {
            return universityName;
        }

        public String getDegreeCode() {
            return degreeCode;
        }

        public IngressionType getIngressionType() {
            return ingressionType;
        }

        public Integer getSlots() {
            return slots;
        }

        @Override
        public boolean equals(Object o) {
            UniversityInfo ui = (UniversityInfo) o;
            return ui.getUniversityName().equals(this.getUniversityName()) && ui.getDegreeCode().equals(this.getDegreeCode());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getUniversityName(), getDegreeCode());
        }
    }
}