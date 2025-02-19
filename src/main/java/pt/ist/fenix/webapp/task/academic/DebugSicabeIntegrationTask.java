package pt.ist.fenix.webapp.task.academic;

import org.apache.poi.ss.usermodel.Row;
import org.fenixedu.academic.FenixEduAcademicConfiguration;
import org.fenixedu.academic.domain.*;
import org.fenixedu.academic.domain.accounting.CustomEvent;
import org.fenixedu.academic.domain.accounting.EventTemplate;
import org.fenixedu.academic.domain.candidacy.IngressionType;
import org.fenixedu.academic.domain.degreeStructure.CycleType;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.RegistrationDataByExecutionYear;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.domain.studentCurriculum.CycleCurriculumGroup;
import org.fenixedu.academic.util.Money;
import org.fenixedu.admissions.domain.Application;
import org.fenixedu.admissions.ist.domain.Utils;
import org.fenixedu.bennu.RenatesIntegrationConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.json.JsonUtils;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.spreadsheet.Spreadsheet;
import org.fenixedu.commons.spreadsheet.SpreadsheetOperator;
import org.fenixedu.connect.domain.ConnectSystem;
import org.fenixedu.connect.domain.Identity;
import org.fenixedu.connect.domain.identification.IdentificationDocument;
import org.fenixedu.connect.domain.identification.PersonalInformation;
import org.fenixedu.connect.domain.identification.TaxInformation;
import org.fenixedu.messaging.core.domain.MessagingSystem;
import org.fenixedu.ulisboa.integration.sas.domain.SasIngressionRegimeMapping;
import org.fenixedu.ulisboa.integration.sas.domain.SchoolLevelTypeMapping;
import org.fenixedu.ulisboa.integration.sas.service.process.AbstractFillScholarshipService;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.dges.schemas.data.sicabe.v1.ObterCandidaturasSubmetidasResponse;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.sicabe.client.SicabeClient;
import pt.ist.sicabe.client.SicabeService;
import pt.ist.sicabe.client.SicabeSheet;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Task(englishTitle = "SicabeIntegrationTask", readOnly = true)
public class DebugSicabeIntegrationTask extends ReadCustomTask implements SpreadsheetOperator {

    private static boolean DEBUG = true;
    private static String NIF = "260136603";

    @Override
    public void runTask() throws Exception {
        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        final LocalDate today = new LocalDate();
        final LocalDate beginDate = executionYear.getBeginDateYearMonthDay().toLocalDate();
        if (beginDate.plusDays(15).isBefore(today)
                && executionYear.getEndDateYearMonthDay().minusDays(45).isAfter(today)) {
            final SicabeSheet inputSheet = generateSheetFor(beginDate.getYear());
            final byte[] input = inputSheet.exportToXLSX();
            output("sicabe" + inputSheet.year + ".xlsx", input);

//            final SicabeSheet outputSheet = SicabeService.sendInfoToSicabe(inputSheet.year, input, username -> User.findByUsername(username)
//                    .getIdentity().getPersonalInformation().getTaxInformation().getTin().substring(2));
//            final byte[] output = outputSheet.exportToXLSX();
//            output("sicabe" + inputSheet.year + "_sendResult.xlsx", output);

//            notifyNoIdentityMatch(inputSheet.year, output);
        }
    }

    private void notifyNoIdentityMatch(final int year, final byte[] output) {
        final Set<String> noIdentityMatch = xlsxRowStream(output, "NoIdentityMatch")
                .skip(1l)
                .map(row -> row.getCell(0).getStringCellValue())
                .collect(Collectors.toSet());
        if (!noIdentityMatch.isEmpty()) {
            final Spreadsheet spreadsheet = new Spreadsheet("NoIdentityMatch");
            xlsxRowStream(output, "Sicabe" + year)
                    .skip(1l)
                    .filter(row -> noIdentityMatch.contains(row.getCell(14).getStringCellValue()))
                    .forEach(row -> {
                        final Row header = row.getSheet().getRow(0);
                        final Spreadsheet.Row r = spreadsheet.addRow();
                        row.forEach(cell -> {
                            final String label = header.getCell(cell.getColumnIndex()).getStringCellValue();
                            if (!"Status".equals(label)) {
                                final String value = cell.getStringCellValue();
                                r.setCell(label, value);
                            }
                        });
                    });

            send("Sicabe_" + year + "_NoIdentityMatch.xlsx", spreadsheet.exportToXLSXSheet(),
                    "Envio Dados SICABE - Problemas",
                    "No ficheiro em anexo estão listadas as candidaturas a bolsas para as quais não foi possível encontrar a identidade da pessoa no Fenix. Devem contactar as pessoas para retificar informação pessoa ou a DGES para contactar o candidato.",
                    "ricardo.rodrigues@tecnico.ulisboa.pt", null);
        }
        final Spreadsheet spreadsheet = new Spreadsheet("Erros");
        final boolean[] hasErrors = {false};
        xlsxRowStream(output, "Erros")
                .skip(1l)
                .forEach(row -> {
                    hasErrors[0] = true;
                    final Row header = row.getSheet().getRow(0);
                    final Spreadsheet.Row r = spreadsheet.addRow();
                    row.forEach(cell -> {
                        final String label = header.getCell(cell.getColumnIndex()).getStringCellValue();
                        if (!"Status".equals(label)) {
                            final String value = cell.getStringCellValue();
                            r.setCell(label, value);
                        }
                    });
                });
        if (hasErrors[0]) {
            send("Sicabe_" + year + "_Erros.xlsx", spreadsheet.exportToXLSXSheet(),
                    "Envio Dados SICABE - Problemas",
                    "No ficheiro em anexo estão listadas erros que impediram a comunicação de alguns dados para o SICABE.",
                    "ricardo.rodrigues@tecnico.ulisboa.pt", null);
        }
        send("Sicabe_" + year + "_ALL.xlsx", output,
                "Envio Dados SICABE - Comunicação",
                "No ficheiro em anexo estão todas as interações com o SICABE.",
                "ricardo.rodrigues@tecnico.ulisboa.pt", null);
    }

    private SicabeSheet generateSheetFor(final int year) {
        final SicabeSheet sicabeSheet = new SicabeSheet(year);

        final ObterCandidaturasSubmetidasResponse obterCandidaturasSubmetidasResponse = SicabeClient.obterCandidaturasSubmetidas(year);
        obterCandidaturasSubmetidasResponse.getCandidaturas().getCandidaturaSubmetida().forEach(candidaturaSubmetida -> {
            final String nif = candidaturaSubmetida.getNif();
            if (DEBUG && nif.equals(NIF)) {
                final String codigoCurso = candidaturaSubmetida.getCodigoCurso();
                final int numeroCandidatura = candidaturaSubmetida.getNumeroCandidatura();
                final String numeroDocumentoIdentificacao = candidaturaSubmetida.getNumeroDocumentoIdentificacao();
                final String nomeCandidato = candidaturaSubmetida.getNomeCandidato().getValue();
                sicabeSheet.spreadsheet.addRow()
                        .setCell("codigoCurso", codigoCurso)
                        .setCell("codigoInstituicaoEnsino", candidaturaSubmetida.getCodigoInstituicaoEnsino())
                        .setCell("curso", candidaturaSubmetida.getCurso())
                        .setCell("dataSubmissao", toString(candidaturaSubmetida.getDataSubmissao()))
                        .setCell("emailTecnico", candidaturaSubmetida.getEmailTecnico().getValue())
                        .setCell("estadoCandidatura.dataAtribuicao", toString(candidaturaSubmetida.getEstadoCandidatura().getDataAtribuicao()))
                        .setCell("estadoCandidatura.descricao", candidaturaSubmetida.getEstadoCandidatura().getDescricao().getValue())
                        .setCell("estadoCandidatura.resultadoEstadoCandidatura", candidaturaSubmetida.getEstadoCandidatura().getResultadoEstadoCandidatura().value())
                        .setCell("estadoCandidatura.valorBolsa", candidaturaSubmetida.getEstadoCandidatura().getValorBolsa())
                        .setCell("instituicaoEnsino", candidaturaSubmetida.getInstituicaoEnsino())
                        .setCell("nif", nif)
                        .setCell("nomeCandidato", nomeCandidato)
                        .setCell("nomeTecnico", candidaturaSubmetida.getNomeTecnico().getValue())
                        .setCell("numeroAluno", candidaturaSubmetida.getNumeroAluno().getValue())
                        .setCell("numeroCandidatura", numeroCandidatura)
                        .setCell("numeroDocumentoIdentificacao", numeroDocumentoIdentificacao)
                        .setCell("tipoDocumentoIdentificacao", candidaturaSubmetida.getTipoDocumentoIdentificacao().value())
                        .setCell("titularidade.titularCET", toString(candidaturaSubmetida.getTitularidade().getValue().isTitularCET()))
                        .setCell("titularidade.titularCSTP", toString(candidaturaSubmetida.getTitularidade().getValue().isTitularCSTP()))
                        .setCell("titularidade.titularDoutoramento", toString(candidaturaSubmetida.getTitularidade().getValue().isTitularDoutoramento()))
                        .setCell("titularidade.titularLicenciatura", toString(candidaturaSubmetida.getTitularidade().getValue().isTitularLicenciatura()))
                        .setCell("titularidade.titularMestrado", toString(candidaturaSubmetida.getTitularidade().getValue().isTitularMestrado()));

                final Identity identity = findByTaxIdentificationNumber(nif);
                final User user = identity == null ? null : identity.getUser();
                final Person person = user == null ? null : user.getPerson();
                final Student student = person == null ? null : person.getStudent();
                final Registration registration = student == null ? null : student.getRegistrationsSet().stream()
                        .filter(r -> codigoCurso.equalsIgnoreCase(r.getDegree().getMinistryCode()))
                        .sorted(Registration.COMPARATOR_BY_START_DATE.reversed())
                        .findFirst().orElse(null);
                final Set<Registration> currentRegistrations = student == null ? Collections.emptySet() : student.getRegistrationsSet().stream()
                        .filter(this::isEnrolledInCurrentYear)
                        .filter(r -> !r.isCanceled())
                        .filter(r -> !r.getDegree().isEmpty())
                        .filter(r -> !r.isConcluded())
                        .collect(Collectors.toSet());
                final Registration degreeChangeRegistration = currentRegistrations.isEmpty() ? null : currentRegistrations.stream()
                        .filter(r -> r != registration)
                        .filter(this::isDegreeChange)
                        .findAny().orElse(null);
                final boolean hasMultipleRegistrations = currentRegistrations.stream().filter(r -> r != degreeChangeRegistration).count() > 1;
                sicabeSheet.spreadsheetAnalise.addRow()
                        .setCell("numeroCandidatura", numeroCandidatura)
                        .setCell("Identity", identity == null ? "" : identity.getExternalId())
                        .setCell("User", user == null ? "" : user.getUsername())
                        .setCell("Student", student == null ? "" : student.getNumber().toString())
                        .setCell("Registration", registration == null ? "" : registration.getDegree().getSigla())
                        .setCell("DegreeChange", degreeChangeRegistration == null ? "" : degreeChangeRegistration.getDegree().getSigla())
                        .setCell("MultipleRegistrations", hasMultipleRegistrations ? currentRegistrations.stream()
                                .filter(r -> r != degreeChangeRegistration)
                                .map(r -> r.getDegree().getSigla())
                                .collect(Collectors.joining(" ; ")) : "")
                        .setCell("OtherRegistrations", hasMultipleRegistrations ? ""
                                : currentRegistrations.stream()
                                .filter(r -> r != degreeChangeRegistration)
                                .filter(r -> r != registration)
                                .map(r -> r.getDegree().getSigla())
                                .collect(Collectors.joining(" ; ")))
                ;

                if (DEBUG) {
                    taskLog("Current Registrations: %s%n", currentRegistrations.size());
                    taskLog("Registration: %s%n", registration != null ? registration.getExternalId() : "BAM!");
                }
                if (identity == null) {
                    if (DEBUG) {
                        taskLog("Identity is null");
                    }
                    final Identity identityByDocNumber = findByDocumentIdentificationNumber(numeroDocumentoIdentificacao);
                    final Identity identityByName = identityByDocNumber == null ? findByName(nomeCandidato) : null;
                    final Identity i = identityByDocNumber == null ? identityByName : identityByDocNumber;
                    final PersonalInformation personalInformation = i == null ? null : i.getPersonalInformation();
                    final IdentificationDocument identificationDocument = personalInformation == null ? null : personalInformation.getIdentificationDocument();
                    final TaxInformation taxInformation = personalInformation == null ? null : personalInformation.getTaxInformation();
                    sicabeSheet.spreadsheetNoIdentityMatch.addRow()
                            .setCell("numeroCandidatura", numeroCandidatura)
                            .setCell("identityByDocNumber", identityByDocNumber == null ? "" : identityByDocNumber.getExternalId())
                            .setCell("identityByName", identityByName == null ? "" : identityByName.getExternalId())
                            .setCell("name", personalInformation == null ? "" : personalInformation.getFullName())
                            .setCell("identificationDocument", identificationDocument == null ? "" : identificationDocument.getDocumentNumber())
                            .setCell("taxInformation", taxInformation == null ? "" : taxInformation.getTin())
                            .setCell("e-mail", i == null ? "" : i.getAccountSet().stream()
                                    .map(account -> account.getEmail())
                                    .collect(Collectors.joining(" ; ")))
                            .setCell("mobile", i == null ? "" : i.getAccountSet().stream()
                                    .map(account -> account.getMobile())
                                    .filter(s -> s != null && !s.isEmpty())
                                    .collect(Collectors.joining(" ; ")))
                    ;
                }

                if (degreeChangeRegistration != null) {
                    if (DEBUG) {
                        taskLog("degreeChangeRegistration");
                    }
                    final String codigoInstituicaoEnsino = RenatesIntegrationConfiguration.getInstitutionCodeProvider().getOrganicUnitCode(degreeChangeRegistration.getLastDegreeCurricularPlan());
                    sicabeSheet.spreadsheetAlteracaoCurso.addRow()
                            .setCell("numeroCandidatura", numeroCandidatura)
                            .setCell("User", user == null ? "" : user.getUsername())
                            .setCell("codigoInstituicaoEnsino", codigoInstituicaoEnsino)
                            .setCell("NovoCurso", degreeChangeRegistration.getDegree().getMinistryCode())
                            .setCell("DataMudança", degreeChangeRegistration.getStartDate().toString("yyyy-MM-dd"));
                }

                currentRegistrations.stream().filter(r -> r == registration || !hasMultipleRegistrations).forEach(currentRegistration -> {
                    if (DEBUG) {
                        taskLog("Registration: %s\t%s%n", currentRegistration.getExternalId(), currentRegistration.getDegreeDescription());
                    }
                    final String codigoInstituicaoEnsino = RenatesIntegrationConfiguration.getInstitutionCodeProvider().getOrganicUnitCode(currentRegistration.getLastDegreeCurricularPlan());
                    final RegistrationDataByExecutionYear dataByExecutionYear = currentRegistration.getRegistrationDataByExecutionYearSet().stream()
                            .filter(data -> data.getExecutionYear() != null && data.getExecutionYear().isCurrent())
                            .findAny().orElseThrow(() -> new Error("Unreachable Code"));
                    forceTuitionCreation(dataByExecutionYear);
                    final SasIngressionRegimeMapping sasIngressionRegimeMapping = getSasIngressionRegimeMapping(currentRegistration);
                    final boolean isInMobility = currentRegistration.isInMobilityState() && currentRegistration.getLastState().getExecutionYear().isCurrent();
                    LocalDate dataMatriculaInscricao = dataByExecutionYear.getEnrolmentDate();
                    if (dataMatriculaInscricao == null && isInMobility) {
                        dataMatriculaInscricao = dataByExecutionYear.getExecutionYear().getBeginLocalDate();
                    }
                    final LocalDate firstPaymentDate = dataMatriculaInscricao == null ? null
                            : dataMatriculaInscricao.isBefore(dataByExecutionYear.getExecutionYear().getBeginLocalDate())
                            ? dataByExecutionYear.getExecutionYear().getBeginLocalDate() : dataMatriculaInscricao;
/*
                        eventsFor(dataByExecutionYear, EventTemplate.Type.TUITION)
                        .flatMap(customEvent -> customEvent.getDueDateAmountMap().keySet().stream())
                        .min(Comparator.naturalOrder())
                        .orElse(null);
 */
                    final long tuitionMonthCount = Math.min(eventsFor(dataByExecutionYear, EventTemplate.Type.TUITION)
                            .flatMap(customEvent -> customEvent.getDueDateAmountMap().keySet().stream())
                            .map(localDate -> localDate.toString("yyyy-MM"))
                            .distinct()
                            .count(), 10l);
                    final Money totalTuition = Stream.concat(eventsFor(dataByExecutionYear, EventTemplate.Type.TUITION),
                                    eventsFor(currentRegistration.getStudent(), currentRegistration.getStudentCurricularPlanStream()
                                            .flatMap(scp -> scp.getEnrolmentStream())
                                            .filter(enrolment -> enrolment.getExecutionYear().isCurrent())
                                            .filter(enrolment -> !enrolment.isAnnulled()), EventTemplate.Type.TUITION))
                            .map(event -> event.getOriginalAmountToPay())
                            .reduce(Money.ZERO, Money::add);
                    final Money totalExempt = Stream.concat(eventsFor(dataByExecutionYear, EventTemplate.Type.TUITION),
                                    eventsFor(currentRegistration.getStudent(), currentRegistration.getStudentCurricularPlanStream()
                                            .flatMap(scp -> scp.getEnrolmentStream())
                                            .filter(enrolment -> enrolment.getExecutionYear().isCurrent())
                                            .filter(enrolment -> !enrolment.isAnnulled()), EventTemplate.Type.TUITION))
                            .map(event -> new Money(event.getDebtInterestCalculator(new DateTime()).getDebtExemptionAmount()))
                            .reduce(Money.ZERO, Money::add);
                    final Money tuitionAmount = totalTuition.subtract(totalExempt);
                    final double enrolledECTS = currentRegistration.getStudentCurricularPlanStream()
                            .flatMap(scp -> scp.getEnrolmentStream())
                            .filter(enrolment -> enrolment.getExecutionYear().isCurrent())
                            .filter(enrolment -> !enrolment.isAnnulled())
                            .mapToDouble(enrolment -> enrolment.getEctsCredits())
                            .sum() + (isInMobility ? 30d : 0d);
                    final long numeroMatriculas = countMatriculas(currentRegistration);
                    final Collection<SchoolLevelType> schoolLevelTypes = Stream.concat(
                                    AbstractFillScholarshipService.completedQualificationsSchoolLevelTypeSupplier.apply(person).stream(),
                                    currentRegistration.getStudent().getRegistrationsSet().stream()
                                            .flatMap(Registration::getStudentCurricularPlanStream)
                                            .flatMap(scp -> scp.getCycleCurriculumGroups().stream())
                                            .filter(group -> group.isConcluded())
                                            .map(this::toSchoolLevelType))
                            .collect(Collectors.toSet());
                    final ExecutionYear anoInscricaoCurso = calculateAnoInscricaoCiclo(currentRegistration);

                    if (dataMatriculaInscricao == null) {
                        if (DEBUG) {
                            taskLog("dataMatriculaInscricao == null");
                        }
                        sicabeSheet.spreadsheetErrors.addRow()
                                .setCell("numeroCandidatura", numeroCandidatura)
                                .setCell("User", user == null ? "" : user.getUsername())
                                .setCell("Erro", "Não tem data de matrícula no ano letivo");
                    }
                    if (sasIngressionRegimeMapping == null) {
                        if (DEBUG) {
                            taskLog("sasIngressionRegimeMapping == null");
                        }
                        sicabeSheet.spreadsheetErrors.addRow()
                                .setCell("numeroCandidatura", numeroCandidatura)
                                .setCell("User", user == null ? "" : user.getUsername())
                                .setCell("Erro", "Não tem código de ingresso na matrícula ou no mapeamento sicabe");
                    }

                    if (dataMatriculaInscricao != null && sasIngressionRegimeMapping != null) {
                        if (DEBUG) {
                            taskLog("dataMatriculaInscricao != null && sasIngressionRegimeMapping != null");
                        }
                        sicabeSheet.spreadsheetRegistoMatriculaInscricao.addRow()
                                .setCell("numeroCandidatura", numeroCandidatura)
                                .setCell("User", user == null ? "" : user.getUsername())
                                .setCell("codigoInstituicaoEnsino", codigoInstituicaoEnsino)
                                .setCell("codigoCurso", currentRegistration.getDegree().getMinistryCode())
                                .setCell("dataMatriculaInscricao", dataMatriculaInscricao.toString("yyyy-MM-dd"));

                        if (isFirstTime(currentRegistration)) {
                            if (DEBUG) {
                                taskLog("isFirstTime(currentRegistration)");
                            }
                            sicabeSheet.spreadsheetAtualizacaoPrimeiraVez.addRow()
                                    .setCell("numeroCandidatura", numeroCandidatura)
                                    .setCell("User", user == null ? "" : user.getUsername())
                                    .setCell("anoInscricaoCurso", anoInscricaoCurso.getBeginDateYearMonthDay().getYear())
                                    .setCell("codRegimeIngresso", sasIngressionRegimeMapping == null ? "" : sasIngressionRegimeMapping.getRegimeCode())
                                    .setCell("descRegimeIngresso", sasIngressionRegimeMapping == null ? "" : sasIngressionRegimeMapping.getRegimeCodeWithDescription())
                                    .setCell("codigoCurso", currentRegistration.getDegree().getMinistryCode())
                                    .setCell("codigoInstituicaoEnsino", codigoInstituicaoEnsino)
                                    .setCell("dataInscricaoAnoLectivo", dataMatriculaInscricao == null ? "" : dataMatriculaInscricao.toString("yyyy-MM-dd"))
                                    .setCell("mesPrimeiroPagamento", firstPaymentDate == null ? "" : firstPaymentDate.toString("yyyy-MM"))
                                    .setCell("numeroAluno", currentRegistration.getStudent().getNumber())
                                    .setCell("numeroAnosCurso", currentRegistration.getCurricularYear())
                                    .setCell("numeroECTSActualInscrito", enrolledECTS)
                                    .setCell("numeroMatriculas", Long.toString(numeroMatriculas))
                                    .setCell("numeroMesesPropina", Long.toString(tuitionMonthCount))
                                    .setCell("observacoes", isInMobility ? "Em Mobilidade" : "")
                                    .setCell("regime", dataByExecutionYear.getMaxCreditsPerYear() == null ? "TEMPO_INTEGRAL" : "TEMPO_PARCIAL")
                                    .setCell("titularCET", toString(schoolLevelTypes.stream().anyMatch(SchoolLevelTypeMapping::isCET)))
                                    .setCell("titularCSTP", toString(schoolLevelTypes.stream().anyMatch(SchoolLevelTypeMapping::isCTSP)))
                                    .setCell("titularDoutoramento", toString(schoolLevelTypes.stream().anyMatch(SchoolLevelTypeMapping::isPhd)))
                                    .setCell("titularLicenciatura", toString(schoolLevelTypes.stream().anyMatch(SchoolLevelTypeMapping::isDegree)))
                                    .setCell("titularMestrado", toString(schoolLevelTypes.stream().anyMatch(SchoolLevelTypeMapping::isMasterDegree)))
                                    .setCell("valorPropina", tuitionAmount == null ? "" : tuitionAmount.toPlainString())
                                    .setCell("iInscritoAnoLectivoActual", Boolean.toString(dataMatriculaInscricao != null))
                            ;
                        } else {
                            if (DEBUG) {
                                taskLog("else");
                            }
                            final ExecutionYear previousEnrolledExecutionYear = currentRegistration.getStudent().getRegistrationsSet().stream()
                                    .map(Registration::getRegistrationDataByExecutionYearSet)
                                    .flatMap(Set::stream)
                                    .filter(data -> data.getEnrolmentDate() != null)
                                    .map(RegistrationDataByExecutionYear::getExecutionYear)
                                    .filter(executionYear -> !executionYear.isCurrent())
                                    .max(Comparator.naturalOrder()).orElse(null);
                            final double previousECTS = currentRegistration.getStudent().getRegistrationsSet().stream()
                                    .flatMap(Registration::getStudentCurricularPlanStream)
                                    .flatMap(StudentCurricularPlan::getEnrolmentStream)
                                    .filter(enrolment -> enrolment.getExecutionYear() == previousEnrolledExecutionYear)
                                    .mapToDouble(enrolment -> enrolment.getEctsCredits())
                                    .sum();
                            final double previousECTSApproved = currentRegistration.getStudent().getRegistrationsSet().stream()
                                    .flatMap(Registration::getStudentCurricularPlanStream)
                                    .flatMap(StudentCurricularPlan::getEnrolmentStream)
                                    .filter(enrolment -> enrolment.getExecutionYear() == previousEnrolledExecutionYear)
                                    .filter(enrolment -> enrolment.isApproved())
                                    .mapToDouble(enrolment -> enrolment.getEctsCredits())
                                    .sum();
                            final long degreeChangeCount = currentRegistration.getStudent().getRegistrationsSet().stream()
                                    .filter(this::isDegreeChange)
                                    .count();
                            final LocalDate lastEvaluation = currentRegistration.getStudent().getRegistrationsSet().stream()
                                    .flatMap(Registration::getStudentCurricularPlanStream)
                                    .flatMap(StudentCurricularPlan::getEnrolmentStream)
                                    .filter(enrolment -> enrolment.getExecutionYear() == previousEnrolledExecutionYear)
                                    .flatMap(enrolment -> enrolment.getEvaluationsSet().stream())
                                    .map(ee -> ee.getExamDateYearMonthDay() == null ? ee.getWhenDateTime().toLocalDate() : ee.getExamDateYearMonthDay().toLocalDate())
                                    .max(Comparator.naturalOrder()).orElse(null);
                            final long numberEnrolmentsInCycle = currentRegistration.getStudent().getRegistrationsSet().stream()
                                    .filter(other -> registration != null && other.getDegreeType() == registration.getDegreeType())
                                    .map(Registration::getRegistrationDataByExecutionYearSet)
                                    .flatMap(Set::stream)
                                    .filter(data -> data.getMaxCreditsPerYear() == null)
                                    .map(RegistrationDataByExecutionYear::getExecutionYear)
                                    .filter(Objects::nonNull)
                                    .distinct()
                                    .count();

                            sicabeSheet.spreadsheetAtualizacaoRestantesCasos.addRow()
                                    .setCell("numeroCandidatura", numeroCandidatura)
                                    .setCell("User", user == null ? "" : user.getUsername())
                                    .setCell("anoInscricaoCurso", anoInscricaoCurso.getBeginDateYearMonthDay().getYear())
                                    .setCell("anoLectivoActual", currentRegistration.getCurricularYear())
                                    .setCell("codRegimeIngresso", sasIngressionRegimeMapping == null ? "" : sasIngressionRegimeMapping.getRegimeCode())
                                    .setCell("descRegimeIngresso", sasIngressionRegimeMapping == null ? "" : sasIngressionRegimeMapping.getRegimeCodeWithDescription())
                                    .setCell("codigoCurso", currentRegistration.getDegree().getMinistryCode())
                                    .setCell("codigoInstituicaoEnsino", codigoInstituicaoEnsino)
                                    .setCell("dataConclusaoAtosAcademicosUltimoAnoLectivoInscrito", lastEvaluation == null ? "" : lastEvaluation.toString("yyyy-MM-dd"))
                                    .setCell("dataInscricaoAnoLectivo", dataMatriculaInscricao == null ? "" : dataMatriculaInscricao.toString("yyyy-MM-dd"))
                                    .setCell("mesPrimeiroPagamento", firstPaymentDate == null ? "" : firstPaymentDate.toString("yyyy-MM"))
                                    .setCell("numeroAluno", currentRegistration.getStudent().getNumber())
                                    .setCell("numeroAnosCurso", calculateNumberOfDegreeCurricularYears(currentRegistration))
                                    .setCell("numeroECTSActualInscrito", enrolledECTS)
                                    .setCell("numeroECTSObtidosUltimoAnoInscrito", previousECTSApproved)
                                    .setCell("numeroECTSUltimoAnoInscrito", previousECTS)
                                    .setCell("numeroInscricoesCicloEstudosTempoIntegral", Long.toString(numberEnrolmentsInCycle))
                                    .setCell("numeroMatriculas", Long.toString(numeroMatriculas))
                                    .setCell("numeroMesesPropina", Long.toString(tuitionMonthCount))
                                    .setCell("numeroOcorrenciasMudancaCurso", Long.toString(degreeChangeCount))
                                    .setCell("observacoes", isInMobility ? "Em Mobilidade" : "")
                                    .setCell("presenteAnoMudouDeCurso", Boolean.toString(isDegreeChange(currentRegistration) && currentRegistration.getRegistrationDataByExecutionYearSet().size() == 1))
                                    .setCell("regime", dataByExecutionYear.getMaxCreditsPerYear() == null ? "TEMPO_INTEGRAL" : "TEMPO_PARCIAL")
                                    .setCell("titularCET", toString(schoolLevelTypes.stream().anyMatch(SchoolLevelTypeMapping::isCET)))
                                    .setCell("titularCSTP", toString(schoolLevelTypes.stream().anyMatch(SchoolLevelTypeMapping::isCTSP)))
                                    .setCell("titularDoutoramento", toString(schoolLevelTypes.stream().anyMatch(SchoolLevelTypeMapping::isPhd)))
                                    .setCell("titularLicenciatura", toString(schoolLevelTypes.stream().anyMatch(SchoolLevelTypeMapping::isDegree)))
                                    .setCell("titularMestrado", toString(schoolLevelTypes.stream().anyMatch(SchoolLevelTypeMapping::isMasterDegree)))
                                    .setCell("ultimoAnoInscrito", previousEnrolledExecutionYear == null ? "" : Integer.toString(previousEnrolledExecutionYear.getBeginDateYearMonthDay().getYear()))
                                    .setCell("valorPropina", tuitionAmount == null ? "" : tuitionAmount.toPlainString())
                                    .setCell("iInscritoAnoLectivoActual", Boolean.toString(dataMatriculaInscricao != null))
                                    .setCell("totalECTScursoAtingirGrau", currentRegistration.getLastStudentCurricularPlan().getApprovedEctsCredits())
                            ;
                        }
                    }
                });
            }
        });

        return sicabeSheet;
    }

    private SchoolLevelType toSchoolLevelType(final CycleCurriculumGroup cycleCurriculumGroup) {
        final CycleType cycleType = cycleCurriculumGroup.getCycleType();
        return cycleType == CycleType.FIRST_CYCLE ? SchoolLevelType.DEGREE
                : cycleType == CycleType.SECOND_CYCLE ? SchoolLevelType.MASTER_DEGREE
                : cycleType == CycleType.THIRD_CYCLE ? SchoolLevelType.DOCTORATE_DEGREE
                : null;
    }

    private int calculateNumberOfDegreeCurricularYears(final Registration registration) {
        final float weight = registration.getLastDegreeCurricularPlan().getDegreeStructure().getAcademicPeriod().getWeight();
        return (int) Math.ceil(weight);
    }

    private ExecutionYear calculateAnoInscricaoCiclo(final Registration registration) {
        return registration.getStudent().getRegistrationsSet().stream()
                .filter(other -> other.getDegreeType() == registration.getDegreeType())
                .filter(other -> !other.isCanceled())
                .map(Registration::getRegistrationDataByExecutionYearSet)
                .flatMap(Set::stream)
                .map(RegistrationDataByExecutionYear::getExecutionYear)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder()).orElseThrow(() -> new Error("Unreachable code"));
    }

    private int countMatriculas(final Registration registration) {
        return (int) registration.getStudent().getRegistrationsSet().stream()
                .filter(other -> other.getDegreeType() == registration.getDegreeType())
                .filter(other -> !other.isCanceled())
                .map(Registration::getRegistrationDataByExecutionYearSet)
                .flatMap(Set::stream)
                .map(RegistrationDataByExecutionYear::getExecutionYear)
                .filter(Objects::nonNull)
                .distinct().count();
    }

    private boolean isFirstTime(final Registration registration) {
        return registration.getStudent().getRegistrationsSet().stream()
                .filter(other -> other.getDegreeType() == registration.getDegreeType())
                .filter(other -> !other.isCanceled())
                .map(Registration::getRegistrationDataByExecutionYearSet)
                .flatMap(Set::stream)
                .map(RegistrationDataByExecutionYear::getExecutionYear)
                .filter(Objects::nonNull)
                .allMatch(ExecutionYear::isCurrent);
    }

    private String toString(final XMLGregorianCalendar date) {
        return date == null ? null : "" + date.getYear()
                + "-" + (date.getMonth() < 10 ? "0" : "") + date.getMonth()
                + "-" + (date.getDay() < 10 ? "0" : "") + date.getDay();
    }

    private String toString(final Boolean b) {
        return b == null ? null : b.toString();
    }

    private Identity findByTaxIdentificationNumber(final String nif) {
        return ConnectSystem.getInstance().getIdentitySet().stream()
                .filter(identity -> matchTaxIdentificationNumber(identity, nif))
                .findAny()
                .orElse(null);
    }

    private Identity findByDocumentIdentificationNumber(final String numeroDocumentoIdentificacao) {
        return ConnectSystem.getInstance().getIdentitySet().stream()
                .filter(identity -> matchDocumentIdentificationNumber(identity, numeroDocumentoIdentificacao))
                .findAny()
                .orElse(null);
    }

    private Identity findByName(final String name) {
        return ConnectSystem.getInstance().getIdentitySet().stream()
                .filter(identity -> matchName(identity, name))
                .findAny()
                .orElse(null);
    }

    private boolean matchTaxIdentificationNumber(final Identity identity, final String nif) {
        final PersonalInformation personalInformation = identity.getPersonalInformation();
        final TaxInformation taxInformation = personalInformation == null ? null : personalInformation.getTaxInformation();
        return taxInformation != null && taxInformation.getTin() != null && taxInformation.getTin().endsWith(nif);
    }

    private boolean matchDocumentIdentificationNumber(final Identity identity, String numeroDocumentoIdentificacao) {
        final PersonalInformation personalInformation = identity.getPersonalInformation();
        final IdentificationDocument identificationDocument = personalInformation == null ? null
                : personalInformation.getIdentificationDocument();
        return identificationDocument != null && identificationDocument.getDocumentNumber() != null
                && identificationDocument.getDocumentNumber().equals(numeroDocumentoIdentificacao);
    }

    private boolean matchName(final Identity identity, final String name) {
        final PersonalInformation personalInformation = identity.getPersonalInformation();
        return personalInformation != null && StringNormalizer.normalizeAndRemoveAccents(personalInformation.getFullName())
                .toLowerCase().equals(StringNormalizer.normalizeAndRemoveAccents(name).toLowerCase());
    }

    private boolean isEnrolledInCurrentYear(final Registration registration) {
        return registration.getRegistrationDataByExecutionYearSet().stream()
                .anyMatch(data -> data.getExecutionYear().isCurrent());
    }

    private boolean isDegreeChange(final Registration registration) {
        final IngressionType ingressionType = registration.getIngressionType();
        return ingressionType != null && ingressionType.getDescription().anyMatch(s -> s.indexOf("Mudança") >= 0 && s.indexOf("Curso") > 0);
    }

    private SasIngressionRegimeMapping getSasIngressionRegimeMapping(final Registration registration) {
        final IngressionType ingressionType = ingressionType(registration);
        return ingressionType == null ? null : ingressionType.getSasIngressionRegimeMapping();
    }

    private IngressionType ingressionType(final Registration registration) {
        final IngressionType ingressionType = registration.getIngressionType();
        final Registration source = registration.getSourceRegistration();
        final IngressionType result = ingressionType == null && source != null
                && source.getSourceRegistration() != registration ? ingressionType(source) : ingressionType;
        if (result == null) {
            final Identity identity = registration.getPerson().getUser().getIdentity();
            if (identity != null) {
                final Supplier<Stream<Application>> supplier =
                        () -> identity.getAccountSet().stream().flatMap(account -> account.getApplicationSet().stream())
                                .filter(application -> Utils.registrationFor(application) == registration);
                if (supplier.get().anyMatch(
                        application -> Utils.isReinstatement(application.getAdmissionProcessTarget().getAdmissionProcess()))) {
                    return Bennu.getInstance().getIngressionTypesSet().stream().filter(type -> type.isReIngression()).findAny()
                            .orElse(null);
                }
                if (supplier.get().anyMatch(application -> application.getAdmissionProcessTarget().getAdmissionProcess().getTags()
                        .anyMatch(s -> s.startsWith("Mudança Curso")))) {
                    return IngressionType.findIngressionTypeByCode("MPIC").orElse(null);
                }
            }
        }
        return result;
    }

    private static Stream<CustomEvent> eventsFor(final RegistrationDataByExecutionYear dataByExecutionYear,
                                                 final EventTemplate.Type type) {
        return dataByExecutionYear.getRegistration().getStudent().getPerson().getEventsSet().stream()
                .filter(CustomEvent.class::isInstance)
                .map(CustomEvent.class::cast)
                .filter(event -> type.isType(event))
                .filter(event -> !event.isCancelled())
                .filter(event -> registrationDataByExecutionYearFor(event) == dataByExecutionYear);
    }

    private static Stream<CustomEvent> eventsFor(final Student student, final Stream<Enrolment> enrolmentStream,
                                                 final EventTemplate.Type type) {
        final Set<Enrolment> enrolments = enrolmentStream.collect(Collectors.toSet());
        return student.getPerson().getEventsSet().stream()
                .filter(CustomEvent.class::isInstance)
                .map(CustomEvent.class::cast)
                .filter(event -> type.isType(event))
                .filter(event -> !event.isCancelled())
                .filter(event -> enrolments.stream().anyMatch(enrolment ->
                        enrolment.getExecutionPeriod() == executionSemester(event)
                                && enrolment.getCurricularCourse() == curricularCourseFor(event)));
    }

    private static ExecutionSemester executionSemester(final CustomEvent event) {
        return JsonUtils.toDomainObject(event.getConfigObject(), "executionSemester");
    }

    public static RegistrationDataByExecutionYear registrationDataByExecutionYearFor(final CustomEvent event) {
        return JsonUtils.toDomainObject(event.getConfigObject(), "registrationDataByExecutionYear");
    }

    private static CurricularCourse curricularCourseFor(final CustomEvent event) {
        return JsonUtils.toDomainObject(event.getConfigObject(), "curricularCourse");
    }

    private void forceTuitionCreation(final RegistrationDataByExecutionYear dataByExecutionYear) {
        if (dataByExecutionYear != null) {
            FenixFramework.atomic(() -> {
                try {
                    Authenticate.mock(dataByExecutionYear.getRegistration().getPerson().getUser(), "Script");
                    final EventTemplate eventTemplate = EventTemplate.templateFor(dataByExecutionYear);
                    if (eventTemplate != null) {
                        taskLog("forceTuitionCreation for %s%n", dataByExecutionYear.getRegistration().getPerson().getUsername());
                        final LocalDate enrolmentDate = dataByExecutionYear.getEnrolmentDate();
                        if (enrolmentDate == null || !enrolmentDate.plusWeeks(2).isBefore(new LocalDate())) {
                            dataByExecutionYear.edit(dataByExecutionYear.getExecutionYear().getBeginLocalDate().plusDays(2), eventTemplate);
                        }
                        eventTemplate.createEventsFor(dataByExecutionYear);
                        dataByExecutionYear.edit(enrolmentDate, eventTemplate);
                    }
                } finally {
                    Authenticate.unmock();
                }
            });
        }
    }

    private static void send(final String filename, final byte[] byteArray, final String subject, final String body,
                             final String emailAddresses, final String emailAddressesBcc) {
        final Properties properties = new Properties();
        properties.put("mail.smtp.host", FenixEduAcademicConfiguration.getConfiguration().getMailSmtpHost());
        properties.put("mail.smtp.name", FenixEduAcademicConfiguration.getConfiguration().getMailSmtpName());
        properties.put("mailSender.max.recipients", FenixEduAcademicConfiguration.getConfiguration().getMailSenderMaxRecipients());
        properties.put("mail.debug", "false");
        final Session session = Session.getDefaultInstance(properties, null);

        final org.fenixedu.messaging.core.domain.Sender sender = MessagingSystem.systemSender();

        final Message message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(sender.getAddress()));
            if (emailAddresses != null) {
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddresses));
            }
            if (emailAddressesBcc != null) {
                message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(emailAddressesBcc));
            }
            message.setSubject(subject);

            final Multipart multipart = new MimeMultipart();

            final BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(body);
            multipart.addBodyPart(messageBodyPart);

            final MimeBodyPart fileBodyPart = new MimeBodyPart();
            final DataSource source = new ByteArrayDataSource(byteArray, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            fileBodyPart.setDataHandler(new DataHandler(source));
            fileBodyPart.setFileName(filename);
            multipart.addBodyPart(fileBodyPart);

            message.setContent(multipart);

            Transport.send(message);
        } catch (final MessagingException ex) {
            throw new Error(ex);
        }
    }

}