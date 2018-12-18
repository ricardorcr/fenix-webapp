package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionSemester;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.studentCurriculum.CurriculumModule;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.bennu.spring.BennuSpringContextHelper;
import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.registration.process.domain.DeclarationTemplate;
import pt.ist.registration.process.handler.CandidacySignalHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
public class GenerateSub23ForAllStudentsTask extends CustomTask {
    private static final String FILENAME = "/afs/ist.utl.pt/ciist/fenix/fenix015/ist/sas.txt";

    private Set<String> renewalStudents;
    private DeclarationTemplate withoutGrant;
    private DeclarationTemplate renewalGrant;
    private DeclarationTemplate withGrant;
    private CandidacySignalHandler bean;
    private String executionYearName;

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    private Set<String> loadRenewalStudents() {
        final File file = new File(FILENAME);
        if (!file.exists()) {
            return new HashSet<>();
        }
        try {
            return new HashSet<>(Files.readAllLines(file.toPath()));
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    @Override
    public void runTask() throws Exception {
        executionYearName = ExecutionYear.readCurrentExecutionYear().getName().replaceAll("/", "-");
        withoutGrant = FenixFramework.getDomainObject("1696928693747715");
        renewalGrant = FenixFramework.getDomainObject("1696928693747716");
        withGrant = FenixFramework.getDomainObject("1696928693747717");
//        renewalStudents = loadRenewalStudents();
        bean = BennuSpringContextHelper.getBean(CandidacySignalHandler.class);

        generateForAllStudents();
    }


    protected void generateForAllStudents() {
        final LocalDate today = new LocalDate();

        //long c =
        ExecutionSemester.readActualExecutionSemester().getEnrolmentsSet().stream().map
                (CurriculumModule::getRegistration)
                .distinct()
                .filter(r -> r.getDegreeType().isBolonhaDegree() || r.getDegreeType().isBolonhaMasterDegree() || r
                        .getDegreeType().isIntegratedMasterDegree())
                .filter(r -> r.getPerson().getExpirationDateOfDocumentIdYearMonthDay() != null)
                .filter(r -> r.getPerson().getDateOfBirthYearMonthDay() != null).filter(r -> r.getPerson().getCountry() != null)
                .filter(r -> r.getPerson().getIdDocumentType() != null)
                .filter(r -> r.getPerson().getDocumentIdNumber() != null)
                .filter(r -> isSubWayClient(r.getPerson().getDateOfBirthYearMonthDay(), today))
                .filter(r -> doesNotHaveAnySub23Declaration(r))
                .peek(r -> taskLog(r.getPerson().getUsername()))
                //.count();
                .forEach(this::send);
        //taskLog("%d%n", c);
    }

    private boolean doesNotHaveAnySub23Declaration(Registration r) {
        return !(hasDeclarationFor(renewalGrant, r) || hasDeclarationFor(withoutGrant, r) || hasDeclarationFor(withGrant, r));
    }

    private boolean hasDeclarationFor(DeclarationTemplate declarationTemplate, Registration registration) {
        final String language = declarationTemplate.getLocale().getLanguage();
        String sigla = registration.getDegree().getSigla();
        String filename = String.format(declarationTemplate.getFilenameFormat(), executionYearName,
                language, sigla, registration.getPerson().getUsername());
        return registration.getRegistrationDeclarationFileSet().stream()
                .anyMatch(f -> f.getExecutionYear().isCurrent() && f.getFilename().equals(filename));
    }

    private boolean isSubWayClient(final YearMonthDay ymd, final LocalDate today) {
        return ymd.plusYears(24).isAfter(today);
    }

    private class Worker extends Thread {

        private String registrationId;

        public Worker(final String registrationId) {
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
            DeclarationTemplate template = getTemplateFor(registration);
            bean.sendDocumentToBeSigned(registration, ExecutionYear.readCurrentExecutionYear(), template);
            return null;
        }
    }

    private void send(Registration registration) {
        final Worker w = new Worker(registration.getExternalId());
        w.start();
        try {
            w.join();
        } catch (InterruptedException ex) {
            throw new Error(ex);
        }
    }

    private DeclarationTemplate getTemplateFor(Registration registration) {
        return withoutGrant;
//        String username = registration.getPerson().getUsername();
//        return renewalStudents.contains(username) ? renewalGrant : withoutGrant;
    }
}