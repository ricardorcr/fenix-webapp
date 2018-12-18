package pt.ist.fenix.webapp;

import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.bennu.spring.BennuSpringContextHelper;
import org.joda.time.LocalDate;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.registration.process.domain.DeclarationTemplate;
import pt.ist.registration.process.handler.CandidacySignalHandler;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */

public class GenerateDeclarationsForAllStudentsTask extends CustomTask {
    private DeclarationTemplate ptDeclaration;
    private DeclarationTemplate enDeclaration;
    private String executionYearName;
    private CandidacySignalHandler bean;
    private LocalDate firstGeneration;
    private Locale PT = Locale.forLanguageTag("pt-PT");
    private Locale EN = Locale.forLanguageTag("en-GB");
    
    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        firstGeneration = new LocalDate(2020, 9, 13);
        ptDeclaration = getTemplateFor("declaracao-matricula", PT);
        enDeclaration = getTemplateFor("declaracao-matricula", EN);
        executionYearName = ExecutionYear.readCurrentExecutionYear().getName().replaceAll("/", "-");
        bean = BennuSpringContextHelper.getBean(CandidacySignalHandler.class);
        taskLog("%s%n", firstGeneration.toString());
        generateForAllStudents();
//        send(User.findByUsername("ist189471").getPerson().getStudent().getLastRegistration());
//        taskLog("ist186730 ia gerar novos docs? %s%n",
//                isToGenerate(User.findByUsername("ist197251").getPerson().getStudent().getLastRegistration()));
    }

    protected void generateForAllStudents() {
//        long c =
        ExecutionSemester.readActualExecutionSemester().getEnrolmentsSet().stream().map(CurriculumModule::getRegistration)
                .distinct()
                .filter(r -> r.getDegreeType().isBolonhaDegree() || r.getDegreeType().isBolonhaMasterDegree() ||
                        r.getDegreeType().isIntegratedMasterDegree())
                .filter(r -> r.getPerson().getExpirationDateOfDocumentIdYearMonthDay() != null)
                .filter(r -> r.getPerson().getDateOfBirthYearMonthDay() != null)
                .filter(r -> r.getPerson().getCountry() != null)
                .filter(r -> r.getPerson().getIdDocumentType() != null)
                .filter(r -> r.getPerson().getDocumentIdNumber() != null)
                .filter(this::isToGenerate)
                .peek(r -> taskLog(r.getPerson().getUsername()))
//                .count();
                .forEach(this::send);
//        taskLog("%d%n", c);
    }

    private boolean hasDeclarationFor(DeclarationTemplate declarationTemplate, Registration registration) {
        final String language = declarationTemplate.getLocale().getLanguage();
        String sigla = registration.getDegree().getSigla();
        String filename = String.format(declarationTemplate.getFilenameFormat(), executionYearName, language, sigla,
                registration.getPerson().getUsername());
        return registration.getRegistrationDeclarationFileSet().stream()
                .anyMatch(f -> f.getCreationDate().toLocalDate().isAfter(firstGeneration) && f.getExecutionYear().isCurrent()
                        && f.getFilename().equals(filename));
    }

    private boolean isToGenerate(Registration r) {
        return !hasDeclarationFor(ptDeclaration, r) || !hasDeclarationFor(enDeclaration, r);
    }

    private DeclarationTemplate getTemplateFor(String name, Locale locale) {
        return DeclarationTemplate.findByNameAndLocale(name, locale).get();
    }

    private void send(Registration registration) {
        final BothWorker w = new BothWorker(registration.getExternalId());
        w.start();
        try {
            w.join();
        } catch (InterruptedException ex) {
            throw new Error(ex);
        }
    }

    private class BothWorker extends Thread {
        private String registrationId;

        public BothWorker(final String registrationId) {
            this.registrationId = registrationId;
        }

        @Override
        public void run() {
            try {
                FenixFramework.getTransactionManager().withTransaction(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        doStuff();
                        return null;
                    }
                }, new AtomicInstance(TxMode.READ, true));
            } catch (Exception e) {
                throw new Error(e);
            }
        }

        private Object doStuff() {
            final Registration registration = FenixFramework.getDomainObject(registrationId);
            Stream.of(ptDeclaration, enDeclaration)
                    .filter(template -> !hasDeclarationFor(template, registration))
                    .forEach(template -> {
                        taskLog(">>>%s->%s%n", registration.getPerson().getUsername(), template.getLocale().getLanguage());
                        bean.sendDocumentToBeSigned(registration, ExecutionYear.readCurrentExecutionYear(), template);
                    });
            return null;
        }
    }
}