package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.Enrolment;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.Event;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.academic.domain.accounting.PostingRule;
import org.fenixedu.academic.domain.accounting.ServiceAgreementTemplate;
import org.fenixedu.academic.domain.accounting.calculator.DebtInterestCalculator;
import org.fenixedu.academic.domain.accounting.events.EventExemptionJustificationType;
import org.fenixedu.academic.domain.accounting.events.gratuity.EnrolmentGratuityEvent;
import org.fenixedu.academic.domain.accounting.events.gratuity.PartialRegimeEvent;
import org.fenixedu.academic.domain.accounting.paymentPlanRules.IsAlienRule;
import org.fenixedu.academic.domain.accounting.postingRules.gratuity.EnrolmentGratuityPR;
import org.fenixedu.academic.domain.accounting.postingRules.gratuity.PartialRegimePR;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FixStandalonePostingRulesDatesAndAssociatedEvents extends CustomTask {

    private final LocalDate START_DATE = new LocalDate(2019, 9, 01);
    private final String SAME_DAY = "sameDay";
    private final String LAST_YEAR = "lastYear";
    User USER = null;

//    @Override
//    public Atomic.TxMode getTxMode() {
//        return Atomic.TxMode.READ;
//    }

    @Override
    public void runTask() throws Exception {
        USER = User.findByUsername("ist24616");
        Set<DegreeCurricularPlan> curricularPlans = Bennu.getInstance().getDegreeCurricularPlansSet().stream()
                //.filter(dcp -> !dcp.isEmpty())
                .filter(dcp -> dcp.isBolonhaDegree() || dcp.isEmpty())
                .filter(dcp -> dcp.isFirstCycle() || dcp.isSecondCycle() || dcp.isEmpty())
                .filter(dcp -> dcp.isActive() || dcp.isEmpty())
                .collect(Collectors.toSet());

        for (DegreeCurricularPlan dcp : curricularPlans) {
            fix(dcp);
        }
        throw new Error("Dry run!");
    }

    private void fix(final DegreeCurricularPlan dcp) throws Exception {
        Set<PostingRule> standalonePRs = getPostingRuleByEventTypeAndDate(
                dcp.getServiceAgreementTemplate(), EventType.STANDALONE_PER_ENROLMENT_GRATUITY, START_DATE);

        if (standalonePRs.size() > 2) {
            taskLog("%n%s%n", dcp.getName());
            taskLog("Para curriculares isoladas temos: %s%n", standalonePRs.size());

            //FenixFramework.atomic(() -> {
            treatStandalonePR(standalonePRs);
            //});
        }
    }

    private void treatStandalonePR(final Set<PostingRule> standalonePRs) {
        Set<EnrolmentGratuityPR> prSet = standalonePRs.stream()
                .filter(pr -> pr.getEndDate() == null)
                .map(pr -> (EnrolmentGratuityPR) pr)
                .collect(Collectors.toSet());
        if (prSet.size() == 2) {
            //is ok
            EnrolmentGratuityPR isAlien = null;
            EnrolmentGratuityPR isNormal = null;
            Iterator<EnrolmentGratuityPR> iterator = prSet.iterator();
            while (iterator.hasNext()) {
                EnrolmentGratuityPR next = iterator.next();
                if (next.isForAliens()) {
                    isAlien = next;
                } else {
                    isNormal = next;
                }
            }
            if (isAlien != null && isNormal != null) {
                taskLog("Vou mudar a data de inicio da PR Alien %s de: %s para: %s%n",
                        isAlien.getExternalId(), isAlien.getStartDate(), START_DATE.toDateTimeAtStartOfDay());
                taskLog("Vou mudar a data de inicio da PR Normal %s de: %s para: %s%n",
                        isNormal.getExternalId(), isNormal.getStartDate(), START_DATE.toDateTimeAtStartOfDay());
                changeStartDate(isAlien, isNormal);
                Set<PostingRule> prToFix = new HashSet<>(standalonePRs);
                prToFix.removeAll(prSet);
                Map<PostingRule, String> prMap = new HashMap<>();
                for (PostingRule pr : prToFix) {
                    String type = fixPrDates(pr);
                    prMap.put(pr, type);
                }
                for (PostingRule pr : prToFix) {
                    fix((EnrolmentGratuityPR) pr, prMap.get(pr), isAlien, isNormal);
                }
            } else {
                taskLog("ERRO - Tem 2 mas não batem certo");
            }
        } else {
            taskLog("ERRO - Houston we have a problem");
        }
    }

    private void fix(final EnrolmentGratuityPR postingRule, final String type, final EnrolmentGratuityPR alien, final EnrolmentGratuityPR normal) {
        LocalDate beginDate = postingRule.getStartDate().toLocalDate();
        LocalDate endDate = postingRule.getEndDate().toLocalDate();
        if (SAME_DAY.equals(type)) {
            if (postingRule.getEnrolmentEvaluationEventSet().isEmpty() &&
                    postingRule.getEnrolmentEventSet().isEmpty()) {
                taskLog("Deleting PR %s%n", postingRule.getExternalId());
                postingRule.delete();
            } else {
                taskLog("ERRO - na pode ser....%s%n", postingRule.getExternalId());
            }
        } else if (LAST_YEAR.equals(type)) {
            fixEvents(postingRule, alien, normal, false);
        } else {
            taskLog("ERRO - temos aqui um caso diferente...");
        }
    }

    private void fixEvents(final EnrolmentGratuityPR postingRule, final EnrolmentGratuityPR alien, final EnrolmentGratuityPR normal,
                           final boolean changePR) {
        for (EnrolmentGratuityEvent event : postingRule.getEnrolmentEventSet()) {
            if (event.getWhenOccured().isAfter(START_DATE.toDateTimeAtStartOfDay())) {
                if (event.getEnrolment() == null ||
                        (event.getEnrolment() != null && event.getEnrolment().getExecutionYear() == ExecutionYear.readCurrentExecutionYear())) {
                    if (!event.isCancelled()) {
                        boolean isAlien = new IsAlienRule().isAppliableFor(event.getStudentCurricularPlan(), event.getExecutionYear());
                        if (isAlien) {
                            fixEvent(event, alien, changePR);
                        } else {
                            fixEvent(event, normal, changePR);
                        }
                    }
                }
            }
        }
    }

    private String fixPrDates(final PostingRule postingRule) {
        //so that when the new event is created this PR is not chosen
        LocalDate beginDate = postingRule.getStartDate().toLocalDate();
        LocalDate endDate = postingRule.getEndDate().toLocalDate();
        String type = null;
        if (beginDate.equals(endDate)) {
            type = SAME_DAY;
        } else if (endDate.equals(START_DATE)) {
            type = LAST_YEAR;
        } else {
            throw new Error("ERRO - PR " + postingRule.getExternalId() + " doesn't fit any of the conditions");
        }
        taskLog("Vou mudar a data da PR %s de: %s - para: %s%n",
                postingRule.getExternalId(), postingRule.getEndDate(),
                START_DATE.toDateTimeAtStartOfDay().minus(10000).plus(9999).toString());
        postingRule.deactivate(START_DATE.toDateTimeAtStartOfDay().plus(9999));

        return type;
    }

    private void fixEvent(final Event event, final PostingRule newPostingRule, final boolean changePR) {
        boolean needToChangeEvent = true;
        if (changePR) {
            needToChangeEvent = setPostingRule(event, newPostingRule);
        }
        boolean needToChangePR = false;
        if (needToChangeEvent) {
            needToChangePR = createNewEventAndResolveOld(event, newPostingRule);
        }
        if (needToChangePR && !changePR) {
            setPostingRule(event, newPostingRule);
        }
    }

    private boolean createNewEventAndResolveOld(final Event oldEvent, final PostingRule newPostingRule) {
        try {
            Enrolment enrolment = null;
            if (oldEvent instanceof EnrolmentGratuityEvent) {
                EnrolmentGratuityEvent event = (EnrolmentGratuityEvent) oldEvent;
                enrolment = event.getEnrolment();
                if (enrolment == null) {
                    taskLog("Aluno %s tem dívida UC regime parcial - %s - mas deve ter-se desinscrito%n",
                            event.getPerson().getUsername(), event.getDescription());
                    return false;
                }
            }
            DebtInterestCalculator calculator = oldEvent.getDebtInterestCalculator(new DateTime());
            BigDecimal totalPaidAmount = calculator.getTotalPaidAmount();
            if (totalPaidAmount.compareTo(BigDecimal.ZERO) > 0) {
                //reimburse
//                Money reimbursableAmount = oldEvent.getReimbursableAmount();
                //The one case in this if was of a student who already payed more and the excess was refunded already
                //now the rest must be refunded so that the new debt can be payed
//                if (reimbursableAmount.getAmount().compareTo(totalPaidAmount) != 0) {
//                    taskLog("$$$ O valor reembolsável: %s devia ser igual ao valor pago: %s - %s evento: %s%n",
//                            reimbursableAmount.getAmount(), totalPaidAmount, oldEvent.getPerson().getUsername(), oldEvent.getExternalId());
//                } else {
                //se a dívida está toda paga e o valor da dívida antiga for igual à nova, não fazer nada e trocar a PR!!
                if (!calculator.hasDueDebtAmount()) {
                    if (oldEvent instanceof EnrolmentGratuityEvent) {
                        EnrolmentGratuityEvent event = (EnrolmentGratuityEvent) oldEvent;
                        Money amountToPay = doCalculationForAmountToPay(event, newPostingRule);
                        if (amountToPay.getAmount().compareTo(calculator.getDebtAmount()) == 0) {
                            return true;
                        } else {
                            taskLog("Dívida: Novo valor %s - Valor antigo %s%n", amountToPay.getAmount(), calculator.getDebtAmount());
                        }
                    } else {
                        taskLog("ERRO - todos os eventos deviam ser EnrolmentGratuityEvent: %s %s%n",
                                oldEvent.getExternalId(), oldEvent.getClass().getName());
                    }
                }
                taskLog("O aluno %s já pagou %s para o evento %s - %s foi feito reembolso%n", oldEvent.getPerson().getUsername(),
                        totalPaidAmount.toString(), oldEvent.getDescription(), oldEvent.getExternalId());
                oldEvent.refund(USER, EventExemptionJustificationType.CANCELLED,
                        "Foi criado com a regra propinas errada. Foi criado um novo evento.", totalPaidAmount);
//                }
            } else if (!oldEvent.isClosed()) {
                taskLog("O evento %s - %s foi cancelado para o aluno %s%n", oldEvent.getExternalId(), oldEvent.getDescription(),
                        oldEvent.getPerson().getUsername());
                //cancel
                oldEvent.cancel(USER.getPerson(),
                        "Foi criado com a regra propinas errada. Foi criado um novo evento.");
            } else {
                //if has no payments and the event is closed, nothing to do here
                return false;
            }
            if (oldEvent instanceof EnrolmentGratuityEvent) {
                EnrolmentGratuityEvent event = (EnrolmentGratuityEvent) oldEvent;
                boolean isAlien = new IsAlienRule().isAppliableFor(event.getStudentCurricularPlan(), event.getExecutionYear());
                EnrolmentGratuityEvent.create(enrolment.getPerson(), enrolment, event.getEventType(), isAlien);
            } else {
                PartialRegimeEvent event = (PartialRegimeEvent) oldEvent;
                Registration registration = event.getRegistration();
                boolean isAlien = new IsAlienRule().isAppliableFor(event.getStudentCurricularPlan(), event.getExecutionYear());
                PartialRegimeEvent.create(registration.getPerson(), registration, registration.getLastStudentCurricularPlan(),
                        event.getExecutionYear(), isAlien);
            }
        } catch (Exception e) {
            taskLog("ERRO - a processar o evento %s%n", oldEvent.getExternalId());
            throw e;
        }
        return false;
    }

    private boolean setPostingRule(final Event event, final PostingRule postingRule) {
        boolean needToChangeEvent = true;

        Method[] methodsSetPR = getAllMethodsInHierarchy(EnrolmentGratuityEvent.class);
        EnrolmentGratuityPR oldPr = (EnrolmentGratuityPR) event.getPostingRule();
        EnrolmentGratuityPR newPr = (EnrolmentGratuityPR) postingRule;
        if (oldPr.getAmountPerEcts().compareTo(newPr.getAmountPerEcts()) == 0
                && oldPr.getNumberOfDaysToStartApplyingInterest() == newPr.getNumberOfDaysToStartApplyingInterest()
                && oldPr.isForAliens() == newPr.isForAliens()
                && oldPr.getEntryType().equals(newPr.getEntryType())
                && oldPr.getEventType().equals(newPr.getEventType())) {
            needToChangeEvent = false;
        }

        taskLog("Changing Event: %s - %s for student %s PR: %s to new PR: %s%n", event.getExternalId(),
                event.getDescription(), event.getPerson().getUsername(),
                event.getPostingRule().getExternalId(), postingRule.getExternalId());
        try {
            for (Method method : methodsSetPR) {
                if (method.getName().contains("setEventPosting")) {
                    method.setAccessible(true);
                    method.invoke(event, postingRule);
                }
            }
        } catch (IllegalAccessException e) {
            taskLog("ERRO - Wrong 1 - PR");
            e.printStackTrace();
            throw new Error();
        } catch (InvocationTargetException e) {
            taskLog("ERRO - Wrong 2 - PR");
            e.printStackTrace();
            throw new Error();
        }
        return needToChangeEvent;
    }

    private void changeStartDate(final PostingRule alien, final PostingRule normal) {
        alien.setStartDate(START_DATE.toDateTimeAtStartOfDay());
        normal.setStartDate(START_DATE.toDateTimeAtStartOfDay());
    }

    protected Money doCalculationForAmountToPay(EnrolmentGratuityEvent event, PostingRule postingRule) {
        final BigDecimal ectsCreditsForCurriculum = event.getEcts();
        EnrolmentGratuityPR enrolmentGratuityPR = (EnrolmentGratuityPR) postingRule;
        return new Money(enrolmentGratuityPR.getAmountPerEcts().multiply(ectsCreditsForCurriculum));
    }

    private Set<PostingRule> getPostingRuleByEventTypeAndDate(ServiceAgreementTemplate sat, EventType eventType, LocalDate when) {
        Set<PostingRule> postingRuleSet = new HashSet<>();
        for (final PostingRule postingRule : sat.getPostingRulesSet()) {
            if (postingRule.getEventType() == eventType && isActiveForDate(postingRule, when)) {
                postingRuleSet.add(postingRule);
            }
        }
        return postingRuleSet;
    }

    public boolean isActiveForDate(PostingRule pr, LocalDate when) {
        if (pr.getStartDate().toLocalDate().isAfter(when)) {
            return false;
        } else {
            return (pr.hasEndDate()) ? !when.isAfter(pr.getEndDate().toLocalDate()) : true;
        }
    }

    private Method[] getAllMethodsInHierarchy(Class<?> objectClass) {
        Set<Method> allMethods = new HashSet<Method>();
        Method[] declaredMethods = objectClass.getDeclaredMethods();
        Method[] methods = objectClass.getMethods();
        if (objectClass.getSuperclass() != null) {
            Class<?> superClass = objectClass.getSuperclass();
            Method[] superClassMethods = getAllMethodsInHierarchy(superClass);
            allMethods.addAll(Arrays.asList(superClassMethods));
        }
        for (Method method : declaredMethods) {
            if (method.getName().contains("setEventPostingRule")) {
                allMethods.add(method);
            }
        }
        for (Method method : methods) {
            if (method.getName().contains("setEventPostingRule")) {
                allMethods.add(method);
            }
        }
        return allMethods.toArray(new Method[allMethods.size()]);
    }
}
