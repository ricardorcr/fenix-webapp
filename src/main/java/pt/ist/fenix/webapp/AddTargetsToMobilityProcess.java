package pt.ist.fenix.webapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.candidacyProcess.mobility.MobilityApplicationProcess;
import org.fenixedu.academic.domain.candidacyProcess.mobility.MobilityQuota;
import org.fenixedu.academic.domain.organizationalStructure.UniversityUnit;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import pt.ist.fenixframework.FenixFramework;

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

public class AddTargetsToMobilityProcess extends CustomTask {

    private static final Locale PT = new Locale("pt", "PT");
    private static final Locale EN = new Locale("en", "GB");
    private static final String TARGET_FILENAME = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/erasmus_more_targets_2020_2021_v2.csv";

    @Override
    public void runTask() throws Exception {
        final AdmissionProcess erasmus = FenixFramework.getDomainObject("1415857443963216");
        processTargetFile(erasmus, TARGET_FILENAME);
    }

    private void processTargetFile(final AdmissionProcess admissionProcess, final String targetPath) throws IOException {
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
            createTargets(admissionProcess,k,v);
        });
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

    private UniversityUnit getUniversityUnit(String universityName) {
        ExecutionYear nextYear = ExecutionYear.readCurrentExecutionYear().getNextExecutionYear();
        final MobilityApplicationProcess applicationProcess = MobilityApplicationProcess.getCandidacyProcessByExecutionInterval(MobilityApplicationProcess.class, nextYear);
        final MobilityQuota mobilityQuota = applicationProcess.getApplicationPeriod().getMobilityQuotasSet().stream()
                .filter(mq -> {
                    String content = mq.getMobilityAgreement().getUniversityUnit().getNameI18n().getContent(EN);
                    if (content == null) {
                        content = mq.getMobilityAgreement().getUniversityUnit().getNameI18n().getContent();
                    }
                    return content.replace("\u00a0"," ").replaceAll("  ", " ").trim().equals(universityName);
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
