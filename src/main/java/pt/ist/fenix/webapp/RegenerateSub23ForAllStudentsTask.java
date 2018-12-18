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

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Sérgio Silva (hello@fenixedu.org).
 */
@SuppressWarnings("Duplicates")
public class RegenerateSub23ForAllStudentsTask extends CustomTask {

    private Set<String> renewalStudents;
    private DeclarationTemplate withoutGrant;
    private DeclarationTemplate renewalGrant;
    private DeclarationTemplate withGrant;

    private CandidacySignalHandler bean;
    private ExecutorService executorService;
    private Set<String> users;

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        users = Stream.of("ist193673", "ist193831", "ist182024", "ist187536", "ist193992", "ist192154", "ist178507", "ist178698",
                "ist194040", "ist194043", "is182100", "ist194080", "ist194079", "ist194078", "ist194074", "ist179140",
                "ist194090", "ist181992", "ist182019", "ist194071", "ist19473", "ist194072", "ist194069", "ist191674",
                "ist194066", "ist194060", "ist194061", "ist194057", "ist194056", "ist194052", "ist194049", "ist194045",
                "ist425889", "ist194039", "ist194041", "ist181836", "ist187014").collect(Collectors.toSet());

        withoutGrant = FenixFramework.getDomainObject("1696928693747715");
        renewalGrant = FenixFramework.getDomainObject("1696928693747716");
        withGrant = FenixFramework.getDomainObject("1696928693747717");
        bean = BennuSpringContextHelper.getBean(CandidacySignalHandler.class);
        executorService = Executors.newFixedThreadPool(6);
        generateForAllStudents();
    }

    protected void generateForAllStudents() {
        final LocalDate today = new LocalDate();

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
                .filter(this::isToRegenerate)
                .peek(r -> taskLog("%s%n", r.getPerson().getUsername()))
                .forEach(this::send);

        taskLog("Stopped submitting requests ..., shutting down executor service");
        executorService.shutdown();

        try {
            boolean terminated = false;
            do {
                if (executorService.awaitTermination(40, TimeUnit.MINUTES)) {
                    terminated = true;
                } else {
                    taskLog("Still awaiting termination");
                }
            } while (!terminated);
        } catch (InterruptedException e) {
            throw new Error(e);
        }
        taskLog("done.");
    }

    private boolean isToRegenerate(final Registration registration) {
        return users.contains(registration.getPerson().getUsername());
    }

    private boolean doesNotHaveAnySub23Declaration(Registration r) {
        return r.getRegistrationDeclarationFileSet().stream().noneMatch(f -> f.getExecutionYear().isCurrent() && f.getFilename().contains("sub23"));
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
                throw new Error(e);}
        }

        private Object doStuff() {
            final Registration registration = FenixFramework.getDomainObject(registrationId);
            DeclarationTemplate template = getTemplateFor(registration);
            bean.sendDocumentToBeSigned(registration, ExecutionYear.readCurrentExecutionYear(), template);
            return null;
        }
    }

    private DeclarationTemplate getTemplateFor(Registration registration) {
        return withoutGrant;
    }

    private void send(Registration registration) {
        executorService.execute(new Worker(registration.getExternalId()));
    }
}