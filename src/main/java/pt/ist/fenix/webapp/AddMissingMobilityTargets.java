package pt.ist.fenix.webapp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.fenixedu.academic.domain.Country;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.organizationalStructure.CountryUnit;
import org.fenixedu.academic.domain.organizationalStructure.PartyTypeEnum;
import org.fenixedu.academic.domain.organizationalStructure.UniversityUnit;
import org.fenixedu.academic.domain.student.RegistrationProtocol;
import org.fenixedu.admissions.domain.AdmissionProcess;
import org.fenixedu.admissions.domain.AdmissionProcessTarget;
import org.fenixedu.admissions.domain.AdmissionsSystem;
import org.fenixedu.admissions.ist.wizard.CreateInboundMobilityProcesses;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.i18n.LocalizedString;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class AddMissingMobilityTargets extends CustomTask {

    private static final Locale EN = new Locale("en", "GB");
    private static final Locale PT = new Locale("pt", "PT");
    private static final String LOCAL = "/home/rcro/DocumentsHDD/fenix/candidaturas/";
    private static final String PROD = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/";
    private static final String ERASMUS_DATA_FILENAME = PROD + "erasmus_univs_em_falta_2.xlsx";
    private static final String OUTSIDE_EUROPE_DATA_FILENAME = PROD + "outsideeurope_2021-11-09.xlsx";

    @Override
    public void runTask() throws Exception {
//        addMissingTargets(ERASMUS_DATA_FILENAME);
        addMissingTargets(OUTSIDE_EUROPE_DATA_FILENAME);
    }

    private void addMissingTargets(final String filePath) {
        final Map<RegistrationProtocol, Stream<Row>> map = xlsxRowStream(filePath, "Concursos")
                .filter(row -> registrationProtocol(row) != null)
                .collect(Collectors.toMap(row -> registrationProtocol(row), row -> Stream.of(row), (r1, r2) -> Stream.concat(r1, r2)));

        map.keySet().forEach(key -> {
            final AdmissionProcess admissionProcess = getProcess(key);
            map.get(key).filter(row -> !hasTarget(admissionProcess, row))
                    .forEach(row -> addTarget(admissionProcess, key, row));
        });
    }

    private AdmissionProcess getProcess(final RegistrationProtocol protocol) {
        return AdmissionsSystem.getInstance().getAdmissionProcessSet().stream()
                .filter(ap -> ap.getCreationTemplate() != null)
                .filter(ap -> ap.getCreationTemplate() instanceof CreateInboundMobilityProcesses)
                .filter(ap -> ap.getTitle().getContent().equals(protocol.getDescription().getContent()))
                .findAny().orElse(null);
    }

    private boolean hasTarget(final AdmissionProcess admissionProcess, final Row row) {
        final Degree degree = degree(row);
        final UniversityUnit universityUnit = universityUnit(row);
        if (universityUnit == null) {
            taskLog("Did not found %s\tfor %s\t%s%n", row.getCell(3).getStringCellValue(), row.getCell(2).getStringCellValue(), row.getCell(0).getStringCellValue());
            return true;
        }
        final LocalizedString name = degree.getPresentationNameI18N()
                .append(ls(" (", " ("))
                .append(acceptedLocalesLs(universityUnit.getNameI18n()))
                .append(ls(")", ")"));
        return admissionProcess.getAdmissionProcessTargetSet().stream()
                .anyMatch(target -> target.getName().toString().equals(name.toString()));
    }

    private void addTarget(final AdmissionProcess admissionProcess, final RegistrationProtocol protocol, final Row row) {
        final IngressionType ingressionType = ingressionType(row);
        final UniversityUnit universityUnit = universityUnit(row);
        if (universityUnit == null) {
            taskLog("Did not found %s%n", row.getCell(3).getStringCellValue());
        } else {
            final Degree degree = degree(row);
            final Integer slots = Integer.parseInt(row.getCell(5).getStringCellValue());

            final LocalizedString name = degree.getPresentationNameI18N()
                    .append(ls(" (", " ("))
                    .append(acceptedLocalesLs(universityUnit.getNameI18n()))
                    .append(ls(")", ")"));

            taskLog("%s\t%s%n", admissionProcess.getTitle().getContent(), name.getContent());

            final AdmissionProcessTarget admissionProcessTarget = admissionProcess.createAdmissionProcessTarget(name, slots);

            setOutcome(admissionProcessTarget, degree, protocol, ingressionType, ExecutionYear.readCurrentExecutionYear());
            final JsonArray tagsConfig = new JsonArray();
            final JsonObject tag = new JsonObject();
            tag.addProperty("name", universityUnit.getExternalId());
            tag.add("title", acceptedLocalesLs(universityUnit.getNameI18n()).json());
            tagsConfig.add(tag);
            admissionProcessTarget.setTagsConfig(tagsConfig.toString());
        }
    }

    private void setOutcome(final AdmissionProcessTarget admissionProcessTarget, final Degree degree,
                            final RegistrationProtocol protocol, final IngressionType ingressionType, final ExecutionYear year) {
        final JsonObject outcome = new JsonObject();
        outcome.addProperty("degree", degree.getExternalId());
        outcome.addProperty("protocol", protocol.getExternalId());
        outcome.addProperty("ingressionType", ingressionType.getExternalId());
        outcome.addProperty("year", year.getExternalId());
        outcome.add("actionName", ls("Matrícular", "Enroll").json());

        admissionProcessTarget.setOutcomeConfig(outcome.toString());
    }

    private LocalizedString ls(final String pt, final String en) {
        return new LocalizedString(Locale.forLanguageTag("pt-PT"), pt).with(Locale.forLanguageTag("en-GB"), en);
    }

    private LocalizedString acceptedLocalesLs(LocalizedString ls) {
        String pt = ls.getContent(Locale.forLanguageTag("pt-PT"));
        String en = ls.getContent(Locale.forLanguageTag("en-GB"));
        pt = pt != null ? pt : en;
        en = en != null ? en : pt;
        return ls(pt, en);
    }

    private RegistrationProtocol registrationProtocol(final Row row) {
        final Cell cell = row.getCell(0);
        final String protocolCode = cell.getStringCellValue();
        return Bennu.getInstance().getRegistrationProtocolsSet().stream()
                .filter(p -> p.getCode().equals(protocolCode))
                .findAny().orElse(null);
    }

    private IngressionType ingressionType(final Row row) {
        final Cell cell = row.getCell(1);
        final String ingressionTypeCode = cell.getStringCellValue();
        return IngressionType.findIngressionTypeByCode(ingressionTypeCode).get();
    }

    private CountryUnit country(final Row row) {
        final Cell cell = row.getCell(2);
        final String countryCode = cell.getStringCellValue();
        return CountryUnit.getCountryUnitByCountry(Country.readByTwoLetterCode(countryCode));
    }

    private UniversityUnit universityUnit(final Row row) {
        final Cell cell = row.getCell(3);
        final String universityName = cell.getStringCellValue();
        return country(row).getSubUnits(PartyTypeEnum.UNIVERSITY).stream()
                .map(UniversityUnit.class::cast)
                .filter(unit -> matchName(unit, universityName))
                .findAny().orElse(null);
    }

    private Degree degree(final Row row) {
        final Cell cell = row.getCell(4);
        final String degreeCode = cell.getStringCellValue();
        return Degree.readBySigla(degreeCode);
    }

    private boolean matchName(final UniversityUnit universityUnit, final String name) {
        String content = universityUnit.getNameI18n().getContent(EN);
        boolean result = false;
        if (content != null) {
            result = stringMatch(content, name);
        }
        if (!result) {
            content = universityUnit.getNameI18n().getContent(PT);
            return content == null ? false : stringMatch(content, name);
        }
        return true;
    }

    private boolean stringMatch(String content1, String content2) {
        return content1.replace("\u00a0"," ").replaceAll("  ", " ").trim()
                .equals(content2.replace("\u00a0"," ").replaceAll("  ", " ").trim());
    }

    protected Stream<Row> xlsxRowStream(final String fileUrl, final String sheetName) {
        try {
            final byte[] content = Files.readAllBytes(new File(fileUrl).toPath());
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
            final Workbook workbook = new XSSFWorkbook(inputStream);
            final Sheet sheet = workbook.getSheet(sheetName);
            return sheet == null ? Stream.empty() : StreamSupport.stream(Spliterators.spliteratorUnknownSize(sheet.rowIterator(), Spliterator.ORDERED), false);
        } catch (final NotOfficeXmlFileException | IOException ex) {
            throw new Error(ex);
        }
    }
}
