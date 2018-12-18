package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventState;
import org.fenixedu.academic.domain.accounting.EventState.ChangeStateEvent;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.academic.domain.accounting.Exemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtExemption;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.events.EnrolmentEvaluationEvent;
import org.fenixedu.academic.domain.exceptions.DomainException;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixedu.domain.SapRequest;
import pt.ist.fenixedu.domain.SapRequestType;
import pt.ist.fenixedu.giaf.invoices.EventProcessor;
import pt.ist.fenixedu.giaf.invoices.SapEvent;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class FixSpecialSeasonCancelations extends CustomTask {

    private static final String BUNDLE = "resources.GiafInvoicesResources";
    private static boolean allowCloseToOpen = false;
    Person responsible = null;

    @Override
    public Atomic.TxMode getTxMode() {
        return Atomic.TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        responsible = User.findByUsername("ist24616").getPerson();
        final String[] studentsToFix = new String[]{"73171", "82549", "77053", "73789", "63981", "51012", "89237", "73555", "94294", "84557", "80963", "87716", "82535", "89703", "81376", "69505", "86307", "81792", "78930", "78746", "90330", "76314", "73965", "93867", "80879", "84528", "69670", "65733", "82318", "80946", "78551", "79621", "70117", "89116", "81451", "84530", "84228", "81849", "83517", "76955", "58119", "84917", "86736", "78105", "53039", "76140", "74061", "85215", "90966", "84467", "72463", "86897", "90939", "84041", "63949", "89974", "81958", "75526", "73027", "92044", "87607", "79713", "90154", "91708", "89480", "57921", "78378", "78712", "76288", "81659", "73096", "54632", "87941", "78812", "88050", "78411", "90454", "82472", "93970", "88184", "87805", "93837", "83781", "87131", "92400", "83741", "87486", "83506", "81459", "76056", "76942", "84703", "75155", "79691", "93652", "79651", "86392", "92389", "90463", "87900", "94602", "73121", "70448", "89218", "86906", "83891", "90778", "66088", "69403", "82458", "90961", "67475", "84520", "81372", "68160", "89349", "69791", "78727", "87539", "86537", "79159", "86581", "87144", "86844", "84379", "90046", "83595", "86600", "81757", "75176", "67382", "81353", "66959", "76277", "87471", "77197", "62886", "82446", "69043", "75173", "90629", "83629", "90582", "89616", "77987", "83908", "73134", "72695", "84904", "58772", "81615", "75369", "78127", "71015", "84019", "93045", "85835", "87796", "80786", "68402", "64955", "80907", "90833", "73930", "93349", "76351", "84439", "84462", "81900", "80888", "90114", "79777", "89939", "83874", "65481", "94148", "81581", "75389", "81144", "76158", "83688", "84330", "81492", "78153", "75686", "81819", "74203", "78512", "86633", "87817", "80993", "90435", "80808", "76553", "76389", "87187", "76166", "89702", "81255", "94056", "67602", "93897", "82122", "82530", "82056", "94130", "81842", "81079", "75570", "81263", "87415", "79745", "83401", "80942", "87341", "78394", "81228", "72703", "81101", "78975", "67181", "84541", "79352", "93962", "87526", "74270", "79209", "75895", "81059", "89744", "81432", "88123", "69534", "74345", "83928", "81761", "81808", "78066", "87243", "84545", "58244", "87674", "92360", "70989", "65225", "81109", "83916", "76740", "80849", "63536", "78909", "81080", "90831", "90496", "84208", "78891", "81693", "87302", "94076", "89912", "87021", "84589", "83818", "40872", "90859", "84231", "78699", "81495", "94296", "73383", "86354", "90579", "73370", "78406", "86695", "83495", "81359", "75199", "83431", "87540", "82077", "87621", "86637", "90986", "86407", "90907", "84430", "86471", "73353", "87484", "86671", "58474", "73207", "94129", "84322", "84578", "87481", "63937", "92309", "75386", "81473", "93532", "87509", "84756", "84291", "84506", "86709", "86954", "79724", "87114", "82317", "78171", "81502", "65746", "94585", "65651", "83970", "84535", "76295", "75986", "39806", "88029", "85249", "63682", "81468", "79182", "69320", "65763", "93995", "77008", "65655", "81252", "74244", "88017", "87738", "74281", "93845", "81522", "67364", "78339", "75851", "84705", "92315", "75677", "65552", "81580", "78923", "81001", "79616", "85193", "78573", "76251", "84406", "79639", "87333", "90852", "36670", "87933", "84429", "78737", "87296", "87236", "83968", "77915", "89344", "67738", "86955", "87156", "63572", "91704", "81790", "79743", "78842", "90572", "86360", "78580", "79681", "84058", "81782", "81339", "84563", "94035", "86982", "77063", "90376", "84549", "83725", "75788", "73783", "90912", "78417", "76356", "84842", "91612", "91601", "42022", "84667", "86874", "77905", "74370", "82331", "70678", "69388", "64875", "75785", "92397", "67859", "93570", "75981", "78198", "67881", "77067", "70825", "84335", "66276", "38481", "81686", "75196", "70647", "81310", "84574", "89477", "59024", "77074", "70646", "81956", "87695", "67248", "84273", "84463", "84051", "78149", "90904", "89537", "90050", "79194", "87151", "72656", "84242", "66343", "93797", "87473", "90902", "90034", "84104", "78397", "72901", "94168", "83553", "80951", "83756", "87829", "81560", "81690", "66003", "69352", "69301", "80839", "84556", "93698", "92378", "87316", "63317", "90505", "86719", "67103", "82154", "91514", "82463", "72718", "84591", "78832", "89995", "87731", "74139", "79410", "74153", "85132", "70939", "92458", "67989", "74059", "82543", "79133", "91162", "68542", "87661", "81630", "89808", "70384", "90179", "73474", "87273", "83778", "65442", "76267", "88647", "78677", "89867", "82050", "81165", "70399", "75892", "82507", "85214", "78728", "87787", "92366", "89114", "91062", "69414", "80793", "82405", "86771", "76753", "79602", "78343", "84776", "81025", "82531", "86708", "85962", "94181", "84793", "73941", "78197", "75422", "92326", "81193", "62506", "79243", "73188", "68273", "56560", "84517", "81784", "85064", "75556", "74176", "84286", "76947", "93264", "75992", "84224", "78167", "91787", "81682", "69612", "84300", "75219", "92249", "68445", "92899", "77946", "85652", "78938", "81892", "81762", "87400", "78069", "66407", "90998", "86550", "93263", "75566", "67763", "93244", "80754", "90573", "94077", "81090", "82052", "90188", "87908", "84479", "85186", "62828", "93836", "92310", "90216", "68067", "86365", "88042", "61480", "81014", "75415", "78762", "91727", "78071", "55045", "92831", "78557", "87717", "52008", "78523", "78022", "79655", "87585", "86892", "83703", "92019", "86562", "75834", "73056", "87502", "84154", "83652", "38310", "83869", "80772", "79490", "91745", "84798", "77010", "63361", "81691", "79586", "86396", "78658", "78989", "83394", "70104", "83982", "87986", "77036", "93547", "78226", "87081", "74280", "87290", "84808", "87579", "64833", "89988", "87179", "94084", "78195", "74123", "90651", "92188", "66098", "73311", "86862", "83769", "81132", "90565", "78981", "87336", "93302", "87947", "56562", "25868", "82392", "79650", "92404", "89217", "78126", "78614", "70547", "84323", "81207", "78225", "90881", "89273", "93872", "73974", "79351", "81627", "81501", "94494", "80941", "69799", "81768", "70434", "75973", "86342", "84434", "79751", "76015", "84301", "81434", "69948", "73124", "81965", "82025", "78365", "79674", "78636", "62440", "84400", "80775", "79515", "67843", "81029", "84560", "70632", "81313", "84726", "76960", "77091", "94032", "65277", "70174", "75319", "78002", "80937", "84742", "82407", "77024", "68068", "78657", "81391", "75679", "81785", "60879", "94014", "86283", "81912", "76508", "84437", "51733", "73643", "81238", "58592", "83429", "86668", "79089", "66917", "38583", "81374", "83749", "73093", "86784", "78617", "75819", "90849", "81812", "76434", "87132", "85373", "81336", "90058", "85316", "75710", "68722", "82815", "87650", "78531", "86443", "80813", "76985", "94426", "76226", "76992", "94560", "69973", "81553", "88642", "82012", "62429", "69633", "88151", "81658", "81957", "83440", "69295", "66891", "85244", "84871", "65278", "90775", "70060", "83806", "91797", "76809", "90214", "83459", "78159", "78908", "84602", "90168", "87311", "69740", "87162", "81793", "87936", "93923", "81355", "48936", "86927", "82409", "87741", "79169", "75439", "79778", "86488", "87863", "82400", "81114", "94447", "88192", "76542", "86519", "83590", "78983", "78853", "86705", "84503", "88120", "64975", "71165", "75231", "81816", "91163", "64754", "57067", "73438", "75818", "81934", "79601", "59016", "93553", "87709", "75576", "91603", "74209", "42170", "76453", "86425", "86588", "79659", "82598", "79077", "67769", "85227", "84804", "78242", "65550", "61518", "91818", "78192", "75545", "90229", "85176", "79052", "78323", "77192", "82047", "78511", "83623", "81287", "86819", "72999", "86776", "80767", "56976", "71001", "90863", "76963", "83480", "84740", "84548", "86473", "78280", "83600", "78031", "70120", "79746", "83494", "70971", "67253", "73802", "81566", "81429", "89763", "91989", "87344", "78093", "77017", "79647", "56692", "70261", "63914", "87256", "79668", "84455", "86789", "86504", "84370", "92879", "82485", "71012", "87309", "64728", "84731", "82049", "75321", "73162", "81043", "81525", "90952", "80773", "80835", "78367", "78619", "79671", "90028", "63979", "87901", "82399", "69487", "73477", "69814", "82588", "90500", "82482", "89694", "91871", "90321", "68248", "81514", "78508", "91786", "79100", "89242", "83503", "79516", "76973", "92333", "87458", "83407", "82430", "90962", "78802", "91469", "70589", "58437", "81011", "81746", "53718", "21121", "83942", "62371", "73145", "90113", "94060", "84442", "69504", "85003", "81470", "85202", "92943", "75676", "73291", "70100", "78861", "86512", "91168", "36897", "84681", "81619", "79759", "82518", "92110", "92340", "89505", "87652", "63303", "74161", "86337", "81446", "80752", "73396", "73427", "88092", "82274", "81250", "82433", "81833", "78845", "89813", "53447", "89893", "76522", "84070", "84522", "86938", "77076", "92238", "80919", "81155", "84711", "63908", "83512", "81210", "83888", "66006", "92337", "73333", "68269", "94562", "87646", "86873", "77028", "78709", "76027", "59019", "87058", "79048", "84226", "83985", "84178", "89955", "75363", "84796", "84620", "64686", "74067", "87409", "87845", "63703", "86828", "87221", "81499", "69947", "94133", "78217", "78655", "79125", "83760", "87824", "76235", "81455", "87005", "81641", "79736", "73713", "92348", "81249", "56072", "88020", "87210", "85325", "79457", "86352", "81058", "79773", "80981", "88124", "86399", "52327", "92897", "92673", "85207", "78652", "90854", "84396", "29835", "79180", "84489", "80974", "78779", "85133", "78852", "90870", "82434", "78504", "76751", "89984", "65551", "78744", "73255", "81917", "92246", "66393", "94472", "78864", "82465", "86421", "75779", "72651", "85182", "91188", "91966", "88012", "89850", "75459", "67861", "92369", "79643", "87306", "87759", "51931", "84457", "85349", "87493", "80911", "84448", "87305", "43123", "84759", "79043", "90702", "83915", "87757", "84593", "68508", "82045", "87560", "76359", "81798", "69727", "94136", "73342", "84567", "86998", "90753", "82161", "90387", "79680", "81639", "79021", "63329", "67877", "84426"};

        Arrays.stream(studentsToFix).forEach(s -> fixStudent(s));
    }

    private void fixStudent(final String studentNumber) {
        Student student = Student.readStudentByNumber(Integer.valueOf(studentNumber));
        student.getPerson().getAcademicEvents().stream()
                .filter(EnrolmentEvaluationEvent.class::isInstance)
                .map(e -> (EnrolmentEvaluationEvent) e)
                .filter(e -> e.getEventType().equals(EventType.SPECIAL_SEASON_ENROLMENT))
                .filter(e -> e.getExecutionPeriodName().equalsIgnoreCase("2ºSemestre 2018/2019")
                        || e.getExecutionPeriodName().equalsIgnoreCase("1ºSemestre 2018/2019"))
                .forEach(e -> fixEvent(e));
    }

    private void fixEvent(final EnrolmentEvaluationEvent event) {
        if (event.getExemptionsSet().size() != 1) {
            if (event.getExemptionsSet().size() > 1) {
                taskLog("Ver evento: %s mais do que uma isenção%n", event.getExternalId());
            }
            return;
        }
        Exemption exemption = event.getExemptionsSet().stream().findFirst().get();
        if (exemption.getResponsible() == responsible
                && exemption.getExemptionJustification().getReason().equals("Dívida duplicada e/ou inscrição já não existe")) {

            if (!event.getAccountingTransactionsSet().isEmpty()) {
                taskLog("$$$$$$$$$$$$$$$$$$Evento %s com pagamentos não fazer nada!!%n", event.getExternalId());
                return;
            }

            Set<SapRequest> requestsNA = event.getSapRequestSet().stream()
                    .filter(sr -> sr.getRequestType().equals(SapRequestType.CREDIT))
                    .filter(sr -> sr.getCreditId().equals(exemption.getExternalId()))
                    .collect(Collectors.toSet());
            SapRequest requestNA = null;
            if (requestsNA.size() == 0) {
                taskLog("Não tem request para apagar: %s%n", event.getExternalId());
            } else if (requestsNA.size() != 1) {
                taskLog("###############O evento: %s tem mais do que uma NA em SAP%n", event.getExternalId());
                return;
            } else {
                requestNA = requestsNA.iterator().next();
            }
            delete(exemption, requestNA);
        } else {
            //Foi cancelado com uma isenção não criada pelo script
        }
    }

    private void delete(final Exemption exemption, final SapRequest sr) {
        if (sr == null || !sr.getSent()) {
            FenixFramework.atomic(() -> {
                Signal.clear(EventState.EVENT_STATE_CHANGED);
                if (sr != null) {
                    taskLog("Going to delete request: %s %s and exemption: %s from event: %s%n",
                            sr.getDocumentNumber(), sr.getExternalId(), exemption.getExternalId(), sr.getEvent().getExternalId());
                    sr.delete();
                } else {
                    taskLog("Going to delete exemption: %s from event: %s%n",
                            exemption.getExternalId(), exemption.getEvent().getExternalId());
                }
                exemption.getEvent().open();
                exemption.delete();

                Signal.register(EventState.EVENT_STATE_CHANGED, this::handlerEventStateChange);
                Signal.register(EventState.EVENT_STATE_CHANGED, this::calculateSapRequestsForCanceledEvent);
                Signal.registerWithoutTransaction(EventState.EVENT_STATE_CHANGED, this::processEvent);
            });
        } else if (sr != null) {
            taskLog("NA já foi enviada para SAP (%s): %s - evento %s %n",
                    sr.getEvent().getPerson().getUsername(), sr.getDocumentNumber(), sr.getEvent().getExternalId());
            return;
        }
    }

    private void handlerEventStateChange(final ChangeStateEvent eventStateChange) {
        final Event event = eventStateChange.getEvent();
        final EventState oldState = eventStateChange.getOldState();
        final EventState newState = eventStateChange.getNewState();

        /*
         *  NewValue > |  null  | OPEN | CLOSED | CANCELED
         *  OldValue   |________|______|________|__________
         *     V       |
         *    null     |   OK   |  OK  |   Ex   |   Ex
         *    OPEN     |   Ex   |  OK  |   OK   |   SAP
         *   CLOSED    |   Ex   |  Ok  |   OK   |   OK
         *  CANCELED   |   Ex   |  Ex  |   Ex   |   OK
         */

        if (oldState == newState) {
            // Not really a state change... nothing to be done.
        } else if (oldState == null && newState == EventState.OPEN) {
            // Ack, normal SAP integration will be fine.
        } else if (oldState == EventState.OPEN && newState == EventState.CLOSED) {
            // Ack, normal SAP integration will be fine.
        } else if (oldState == EventState.CLOSED && newState == EventState.OPEN) {
            // Ack, normal SAP integration will be fine.
        } else if (oldState == EventState.CLOSED && newState == EventState.CANCELLED) {
            // Ack, normal SAP integration will be fine.
        } else if (oldState == EventState.OPEN && newState == EventState.CANCELLED) {
            if (!new SapEvent(event).canCancel()) {
                throw new DomainException(Optional.of(BUNDLE), "error.event.state.change.first.in.sap");
            }
        } else if (allowCloseToOpen && oldState == EventState.CLOSED && newState == EventState.OPEN) {
            // Ack.
        } else {
            throw new DomainException(Optional.of(BUNDLE), "error.new.event.state.change.must.be.handled", (oldState == null ?
                    "null" : oldState.name()), (newState == null ? "null" : newState.name()), event.getExternalId());
        }
    }

    private void calculateSapRequestsForCanceledEvent(final ChangeStateEvent eventStateChange) {
        final Event event = eventStateChange.getEvent();
        final EventState oldState = eventStateChange.getOldState();
        final EventState newState = eventStateChange.getNewState();

        if (newState == EventState.CANCELLED && oldState != newState && event.getSapRequestSet().isEmpty()) {
            final DebtInterestCalculator calculator = event.getDebtInterestCalculator(new DateTime());
            final Money debtAmount = new Money(calculator.getDebtAmount());
            if (debtAmount.isPositive()) {
                final DebtExemption debtExemption = calculator.getAccountingEntries().stream()
                        .filter(e -> e instanceof DebtExemption)
                        .map(e -> (DebtExemption) e)
                        .filter(e -> new Money(e.getAmount()).equals(debtAmount))
                        .findAny().orElse(null);
                if (debtExemption == null) {
                    throw new Error("inconsistent data, event is canceled but the the exempt value does not match the orginal debt value");
                }
                final SapEvent sapEvent = new SapEvent(event);
                sapEvent.fakeSapRequest(SapRequestType.INVOICE, "ND0", debtAmount, null);
                sapEvent.fakeSapRequest(SapRequestType.CREDIT, "NA0", debtAmount, debtExemption.getId());
            }
        }
    }

    private void processEvent(final ChangeStateEvent eventStateChange) {
        EventProcessor.calculate(() -> eventStateChange.getEvent());
        EventProcessor.sync(() -> eventStateChange.getEvent());
    }
}
