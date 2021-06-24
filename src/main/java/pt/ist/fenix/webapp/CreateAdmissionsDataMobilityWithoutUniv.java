package pt.ist.fenix.webapp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.IngressionType;
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

public class CreateAdmissionsDataMobilityWithoutUniv extends CustomTask {

    //prod /afs/ist.utl.pt/ciist/fenix/fenix015/ist/
    //local /afs/ist.utl.pt/ciist/fenix/fenix015/ist/
    private static final Locale PT = new Locale("pt", "PT");
    private static final Locale EN = new Locale("en", "GB");
    private static final String DOUBLE_DEGREES_NO_UNIVS_DATA_FILENAME = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/cursos_mobilidade_double_degrees_sem_univs_1s_2020_2021.csv";
    private static final String FORM_DATA_KIC_FILENAME = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/mobilityFormDataKIC.json";
    private static final String OUTCOME_FORM_DATA_FILENAME = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/mobilityOutcomeFormData.json";

    @Override
    public void runTask() throws Exception {
        AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(admissionProcess -> {
                    final String content = admissionProcess.getTitle().getContent(PT);
                    return content.contains("1º semestre 2021/2022");
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

        processTargetFile(DOUBLE_DEGREES_NO_UNIVS_DATA_FILENAME, FORM_DATA_KIC_FILENAME, "eit.innoenergy@tecnico.ulisboa.pt");
    }

    private void processTargetFile(final String targetPath, final String formPath, String email) throws IOException {
        Map<RegistrationProtocol, Set<DegreeInfo>> processTargets = new HashMap<>();
        final List<String> lineData = Files.readAllLines(new File(targetPath).toPath());
        for (String line : lineData) {
            String[] split = line.split("\t");
            String protocolCode = split[0];
            final RegistrationProtocol protocol = Bennu.getInstance().getRegistrationProtocolsSet().stream()
                    .filter(p -> p.getCode().equals(protocolCode))
                    .findAny().orElse(null);
            final Set<DegreeInfo> degreeInfos = processTargets.computeIfAbsent(protocol, (t) -> new HashSet<DegreeInfo>());
            String ingressionTypeCode = split[1];
            final IngressionType ingressionType = IngressionType.findIngressionTypeByCode(ingressionTypeCode).get();
            String degreeCode = split[2];
            final DegreeInfo universityInfo = new DegreeInfo(0, ingressionType, degreeCode);
            degreeInfos.add(universityInfo);
        }

        processTargets.forEach((k,v) -> {
            createMobility(k,v, email, formPath);
        });
    }

    private void createMobility(final RegistrationProtocol protocol, final Set<DegreeInfo> targets, final String email, final String formPath) {
        final LocalizedString title = ls(protocol.getDescription().getContent(PT),protocol.getDescription().getContent(EN));
        final DateTime startDate = new DateTime(2021, 05, 21, 0, 0);
        final DateTime endDate = new DateTime(2021, 06, 20, 23, 59, 59);

        final LocalizedString tags = ls("Mobilidade 2021/2022", "Mobility 2021/2022");
        final String informationUrl = "";

        createMobility(title, startDate, endDate, tags, informationUrl, email, true, protocol, targets, formPath);
    }

    private void createMobility(final LocalizedString title, final DateTime startDate, final DateTime endDate, final LocalizedString tags,
                                final String informationUrl, final String email, final boolean hasAdmissionGranted,
                                final RegistrationProtocol protocol, final Set<DegreeInfo> targets, final String formPath) {
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
        admissionProcess.setOutcomeConfig(getHigherEducationOutcome().toString());
        admissionProcess.setHasAdmissionGranted(hasAdmissionGranted);
        setFormData(admissionProcess, formPath);
        createTargets(admissionProcess, protocol, targets);
    }

    private void createTargets(final AdmissionProcess admissionProcess, final RegistrationProtocol protocol, final Set<DegreeInfo> targets) {
        targets.stream()
                .forEach(degreeInfo -> {
                    final IngressionType ingressionType = degreeInfo.getIngressionType();
                    final Degree degree = Degree.readBySigla(degreeInfo.getDegreeCode());
                    LocalizedString name = ls(degree.getPresentationNameI18N().getContent(PT), degree.getPresentationNameI18N().getContent(EN));
                    final AdmissionProcessTarget admissionProcessTarget = admissionProcess.createAdmissionProcessTarget(
                            name, degreeInfo.getSlots());
                    setOutcome(admissionProcessTarget, degree, protocol, ingressionType);
                });
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

    private class DegreeInfo {

        IngressionType ingressionType;
        Integer slots;
        String degreeCode;

        public DegreeInfo(final Integer slots, final IngressionType ingressionType, final String degreeCode) {
            this.slots = slots;
            this.ingressionType = ingressionType;
            this.degreeCode = degreeCode;
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
            DegreeInfo di = (DegreeInfo) o;
            return di.getDegreeCode().equals(this.getDegreeCode());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getDegreeCode());
        }
    }
}