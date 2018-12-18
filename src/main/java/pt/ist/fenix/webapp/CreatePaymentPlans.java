package pt.ist.fenix.webapp;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.util.List;

import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.accounting.EventType;
import org.fenixedu.academic.domain.accounting.PostingRule;
import org.fenixedu.academic.domain.accounting.ServiceAgreementTemplate;
import org.fenixedu.academic.domain.accounting.installments.InstallmentWithMonthlyPenalty;
import org.fenixedu.academic.domain.accounting.paymentPlans.FullGratuityPaymentPlan;
import org.fenixedu.academic.domain.accounting.paymentPlans.FullGratuityPaymentPlanForAliens;
import org.fenixedu.academic.domain.accounting.postingRules.gratuity.EnrolmentGratuityPR;
import org.fenixedu.academic.domain.accounting.postingRules.gratuity.PartialRegimePR;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.joda.time.DateTime;
import org.joda.time.YearMonthDay;

public class CreatePaymentPlans extends CustomTask {

    ExecutionYear executionYear;
    YearMonthDay limit1 = new YearMonthDay(2020, 11, 8);
    YearMonthDay limit2 = new YearMonthDay(2020, 12, 8);
    YearMonthDay limit3 = new YearMonthDay(2021, 1, 8);
    YearMonthDay limit4 = new YearMonthDay(2021, 2, 8);
    YearMonthDay limit5 = new YearMonthDay(2021, 3, 8);
    YearMonthDay limit6 = new YearMonthDay(2021, 4, 8);
    YearMonthDay limit7 = new YearMonthDay(2021, 5, 8);
    YearMonthDay limit8 = new YearMonthDay(2021, 6, 8);
    YearMonthDay limit9 = new YearMonthDay(2021, 7, 8);
    YearMonthDay limit10 = new YearMonthDay(2021, 8, 8);
    YearMonthDay startDate = new YearMonthDay(2020, 9, 14);  
    
    @Override
    public void runTask() throws Exception {
        executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
        List<String> degreePlans = null;
        try {
            degreePlans = Files.readAllLines(new File("/afs/ist.utl.pt/ciist/fenix/fenix015/ist/fenix_propinas_2020_2021.csv").toPath());
//            degreePlans = Files.readAllLines(new File("/home/rcro/DocumentsHDD/fenix/inscricoes/2020-2021/fenix_propinas_2020_2021.csv").toPath());
        } catch (IOException e) {
            throw new Error("Erro a ler o ficheiro.");
        }
        degreePlans.stream().filter(line -> !line.startsWith("#")).forEach(this::createMainPlan);
        degreePlans.stream().filter(line -> !line.startsWith("#")).forEach(this::createAlienPlans);
        degreePlans.stream().filter(line -> !line.startsWith("#")).forEach(this::createPartialRegimeAndStandalone);        
    }

    private void createMainPlan(String degreePlan) {
        DegreePlanBean degreePlanBean = new DegreePlanBean(degreePlan);
        Degree degree = Degree.readBySigla(degreePlanBean.getDegreeAcronym());        
        List<DegreeCurricularPlan> dcps = degree.getDegreeCurricularPlansForYear(executionYear);
        if (dcps.size() > 1) {
            taskLog("Há vários DCPs para %s%n", degreePlanBean.getDegreeAcronym());
        } else {
            DegreeCurricularPlan dcp = dcps.get(0);
            FullGratuityPaymentPlan paymentPlan = new FullGratuityPaymentPlan(executionYear, dcp.getServiceAgreementTemplate(), true);
            BigDecimal penalty = BigDecimal.valueOf(0.01);
            Integer maxMonths = 99;
            new InstallmentWithMonthlyPenalty(paymentPlan, degreePlanBean.installment1, startDate, limit1, penalty, limit1.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, degreePlanBean.installment2, startDate, limit2, penalty, limit2.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, degreePlanBean.installment3, startDate, limit3, penalty, limit3.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, degreePlanBean.installment4, startDate, limit4, penalty, limit4.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, degreePlanBean.installment5, startDate, limit5, penalty, limit5.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, degreePlanBean.installment6, startDate, limit6, penalty, limit6.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, degreePlanBean.installment7, startDate, limit7, penalty, limit7.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, degreePlanBean.installment8, startDate, limit8, penalty, limit8.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, degreePlanBean.installment9, startDate, limit9, penalty, limit9.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, degreePlanBean.installment10, startDate, limit10, penalty, limit10.plusDays(1), maxMonths);
            
            taskLog("Plano de pagamento principal criado para %s -> payment plan: %s%n", dcp.getName(), paymentPlan.getExternalId());
        }        
    }

    private void createAlienPlans(String degreePlan) {
        DegreePlanBean degreePlanBean = new DegreePlanBean(degreePlan);
        Degree degree = Degree.readBySigla(degreePlanBean.getDegreeAcronym());        
        List<DegreeCurricularPlan> dcps = degree.getDegreeCurricularPlansForYear(executionYear);
        if (dcps.size() > 1) {
            taskLog("Há vários DCPs para %s%n", degreePlanBean.getDegreeAcronym());
        } else {
            DegreeCurricularPlan dcp = dcps.get(0);
            FullGratuityPaymentPlanForAliens paymentPlan = new FullGratuityPaymentPlanForAliens(executionYear, dcp.getServiceAgreementTemplate());
            BigDecimal penalty = BigDecimal.valueOf(0.01);
            Integer maxMonths = 99;
            Money installment = null;
            Money partialTimeFix = null;
            BigDecimal partialTimeEcts = null;
            BigDecimal standaloneCurricular = null;
            if (degreePlanBean.getDegreeAcronym().equals("MOTU")) {                
                installment = new Money(350);
                partialTimeFix = new Money(1400);
                partialTimeEcts = new BigDecimal(23.33).setScale(2, RoundingMode.CEILING);
                standaloneCurricular = new BigDecimal(87.5);
            } else {
                installment = new Money(700);
                partialTimeFix = new Money(2800);
                partialTimeEcts = new BigDecimal(46.67).setScale(2, RoundingMode.FLOOR);
                standaloneCurricular = new BigDecimal(175);
            }
            new InstallmentWithMonthlyPenalty(paymentPlan, installment, startDate, limit1, penalty, limit1.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, installment, startDate, limit2, penalty, limit2.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, installment, startDate, limit3, penalty, limit3.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, installment, startDate, limit4, penalty, limit4.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, installment, startDate, limit5, penalty, limit5.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, installment, startDate, limit6, penalty, limit6.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, installment, startDate, limit7, penalty, limit7.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, installment, startDate, limit8, penalty, limit8.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, installment, startDate, limit9, penalty, limit9.plusDays(1), maxMonths);
            new InstallmentWithMonthlyPenalty(paymentPlan, installment, startDate, limit10, penalty, limit10.plusDays(1), maxMonths);
            
            deactivateExistingPartialRegimePR(EventType.PARTIAL_REGIME_GRATUITY, startDate.toDateTimeAtMidnight(), dcp.getServiceAgreementTemplate(), true);
            new PartialRegimePR(startDate.toDateTimeAtMidnight(), null, dcp.getServiceAgreementTemplate(), partialTimeFix, 60, true);
            
            deactivateExistingEnrolmentGratuityPR(EventType.PARTIAL_REGIME_ENROLMENT_GRATUITY, startDate.toDateTimeAtMidnight(), dcp.getServiceAgreementTemplate(), true);
            new EnrolmentGratuityPR(startDate.toDateTimeAtMidnight(), null, EventType.PARTIAL_REGIME_ENROLMENT_GRATUITY, dcp.getServiceAgreementTemplate(), partialTimeEcts, 60, true);
            
            deactivateExistingEnrolmentGratuityPR(EventType.STANDALONE_PER_ENROLMENT_GRATUITY, startDate.toDateTimeAtMidnight(), dcp.getServiceAgreementTemplate(), true);
            new EnrolmentGratuityPR(startDate.toDateTimeAtMidnight(), null, EventType.STANDALONE_PER_ENROLMENT_GRATUITY, dcp.getServiceAgreementTemplate(), standaloneCurricular, 1, true);
            
            taskLog("Plano de pagamento estrangeiros e regras de propinas criadas para %s -> payment plan: %s%n", dcp.getName(), paymentPlan.getExternalId());
        }        
    }

    private void createPartialRegimeAndStandalone(String degreePlan) {
        DegreePlanBean degreePlanBean = new DegreePlanBean(degreePlan);
        Degree degree = Degree.readBySigla(degreePlanBean.getDegreeAcronym());        
        List<DegreeCurricularPlan> dcps = degree.getDegreeCurricularPlansForYear(executionYear);
        if (dcps.size() > 1) {
            taskLog("Há vários DCPs para %s%n", degreePlanBean.getDegreeAcronym());
        } else {
            DegreeCurricularPlan dcp = dcps.get(0);
            deactivateExistingPartialRegimePR(EventType.PARTIAL_REGIME_GRATUITY, startDate.toDateTimeAtMidnight(), dcp.getServiceAgreementTemplate(), false);
            new PartialRegimePR(startDate.toDateTimeAtMidnight(), null, dcp.getServiceAgreementTemplate(), degreePlanBean.partialTimeFix, 60, false);
            
            deactivateExistingEnrolmentGratuityPR(EventType.PARTIAL_REGIME_ENROLMENT_GRATUITY, startDate.toDateTimeAtMidnight(), dcp.getServiceAgreementTemplate(), false);
            new EnrolmentGratuityPR(startDate.toDateTimeAtMidnight(), null, EventType.PARTIAL_REGIME_ENROLMENT_GRATUITY, dcp.getServiceAgreementTemplate(), degreePlanBean.partialTimeEcts, 60, false);
            
            deactivateExistingEnrolmentGratuityPR(EventType.STANDALONE_PER_ENROLMENT_GRATUITY, startDate.toDateTimeAtMidnight(), dcp.getServiceAgreementTemplate(), false);
            new EnrolmentGratuityPR(startDate.toDateTimeAtMidnight(), null, EventType.STANDALONE_PER_ENROLMENT_GRATUITY, dcp.getServiceAgreementTemplate(), degreePlanBean.standaloneCurricular, 1, false);
            
            taskLog("Regras de tempo parcial e curriculares isoladas criadas para %s%n", dcp.getName());
        }
    }
    
    private static void deactivateExistingPartialRegimePR(final EventType eventType, final DateTime when,
            final ServiceAgreementTemplate serviceAgreementTemplate, boolean forAliens) {

        for (final PostingRule postingRule : serviceAgreementTemplate.getPostingRulesSet()) {
            if (postingRule.getEventType() == eventType && postingRule.isActiveForDate(when)
                    && ((PartialRegimePR)postingRule).isForAliens() == forAliens) {
                postingRule.deactivate(when);
            }
        }
    }
    
    private static void deactivateExistingEnrolmentGratuityPR(final EventType eventType, final DateTime when,
            final ServiceAgreementTemplate serviceAgreementTemplate, boolean forAliens) {

        for (final PostingRule postingRule : serviceAgreementTemplate.getPostingRulesSet()) {
            if (postingRule.getEventType() == eventType && postingRule.isActiveForDate(when)
                    && ((EnrolmentGratuityPR)postingRule).isForAliens() == forAliens) {
                postingRule.deactivate(when);
            }
        }
    }    
    
    class DegreePlanBean {
        String degreeName;
        Money totalValue;
        Money installment1;
        Money installment2;
        Money installment3;
        Money installment4;
        Money installment5;
        Money installment6;
        Money installment7;
        Money installment8;
        Money installment9;
        Money installment10;
        Money partialTimeFix;
        BigDecimal partialTimeEcts;
        BigDecimal standaloneCurricular;

        public DegreePlanBean(String degreePlan) {
            String[] split = degreePlan.split("\t");
            degreeName = split[0];
            totalValue = new Money(split[1]);
            installment1 = new Money(split[2]);
            installment2 = new Money(split[3]);
            installment3 = new Money(split[4]);
            installment4 = new Money(split[5]);
            installment5 = new Money(split[6]);
            installment6 = new Money(split[7]);
            installment7 = new Money(split[8]);
            installment8 = new Money(split[9]);
            installment9 = new Money(split[10]);
            installment10 = new Money(split[11]);
            partialTimeFix = new Money(split[12]);
            partialTimeEcts = new BigDecimal(split[13]);
            standaloneCurricular = new BigDecimal(split[14]);
        }

        public String getDegreeAcronym() {
            return degreeName.split("\\(")[1].replace(")", "");
        }
    }
}
