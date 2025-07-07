package pt.ist.fenix.webapp.task.academic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.fenixedu.academic.domain.Degree;
import org.fenixedu.academic.domain.DegreeCurricularPlan;
import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.caseHandling.Process;
import org.fenixedu.academic.domain.phd.PhdCandidacyProcessState;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcess;
import org.fenixedu.academic.domain.phd.PhdIndividualProgramProcessState;
import org.fenixedu.academic.domain.phd.PhdParticipant;
import org.fenixedu.academic.domain.phd.PhdParticipantBean;
import org.fenixedu.academic.domain.phd.PhdProgramCandidacyProcessState;
import org.fenixedu.academic.domain.phd.PhdStudyPlan;
import org.fenixedu.academic.domain.phd.PhdStudyPlanBean;
import org.fenixedu.academic.domain.phd.candidacy.PhdProgramCandidacyProcessBean;
import org.fenixedu.academic.domain.phd.candidacy.RegistrationFormalizationBean;
import org.fenixedu.academic.dto.person.PersonBean;
import org.fenixedu.admissions.ist.service.AcademicPersonService;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.dynamicForms.DynamicForm;
import org.fenixedu.smartForms.domain.Request;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.FenixFramework;

import java.util.Set;

public class CreateCompletePhdProcess extends CustomTask {

    @Override
    public void runTask() throws Exception {
//        String phdCandidacyRequestId = "1697916536228803";//application.getDataObject().get("phdCandidacyRequestId").getAsString(); //RR
        String phdCandidacyRequestId = "1697916536228805";//application.getDataObject().get("phdCandidacyRequestId").getAsString(); //LC

        final Request phdCandidacy = FenixFramework.getDomainObject(phdCandidacyRequestId);
        final DynamicForm inputForm = phdCandidacy.inputForm(true);

        final PhdProgramCandidacyProcessBean bean = new PhdProgramCandidacyProcessBean();
        final Person requester = AcademicPersonService.personFor(phdCandidacy.getRequester());
        bean.setPersonBean(new PersonBean(requester));
        final DynamicForm.AsyncSelect selectedDegree = inputForm.get("DEGREE");
        final Degree degree = FenixFramework.getDomainObject(selectedDegree.value());
        bean.setDegree(degree);
        bean.setProgram(degree.getPhdProgram());
        bean.setCandidacyDate(phdCandidacy.getLockInstant().toLocalDate());
        //é preciso ter o ano aqui para meter a matrícula no executioDegree certo e ano e tal
        ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
        bean.setExecutionYear(executionYear);

        PhdIndividualProgramProcess phdProcess = Process.createNewProcess(null, PhdIndividualProgramProcess.class,
                bean);
        //orientador interno só é preciso pessoa e instituição (não sei se é preciso no dominio)
        final DynamicForm.Array advisers = inputForm.get("advisers");
        advisers.getData().getAsJsonArray().forEach(advisor -> addAdvisor(phdProcess, advisor));

        //TODO meter no application o ID da phdProcess/matrícula

        PhdStudyPlanBean phdStudyPlanBean = new PhdStudyPlanBean(phdProcess);
        phdStudyPlanBean.setDegree(degree);
        phdStudyPlanBean.setExempted(false);
        PhdStudyPlan phdStudyPlan = new PhdStudyPlan(phdStudyPlanBean);

        LocalDate whenFormalizedRegistration = new LocalDate();
        phdProcess.setWhenFormalizedRegistration(whenFormalizedRegistration);

        RegistrationFormalizationBean formalizationBean = new RegistrationFormalizationBean();
        //isto tem que vir do passo intermédio, quando quer começar a matrícula
        LocalDate whenStudiesStarted = new LocalDate();
        formalizationBean.setWhenStartedStudies(whenStudiesStarted);
        phdProcess.setWhenStartedStudies(whenStudiesStarted);

        registrationFormalization(formalizationBean, requester, phdProcess, executionYear);
//        throw new Error("Dry run");
    }

    private void addAdvisor(final PhdIndividualProgramProcess phdProcess, final JsonElement jsonElement) {
        final JsonObject advisor = jsonElement.getAsJsonObject();
        PhdParticipantBean bean = new PhdParticipantBean();
        if (advisor.get("isIstAdvisor").getAsBoolean()) {
            taskLog("Entrei no IF");
            bean.setParticipantType(PhdParticipantBean.PhdParticipantType.INTERNAL);
            final String username = advisor.get("istAdvisor").getAsJsonObject().get("value").getAsString();
            bean.setPerson(User.findByUsername(username).getPerson());
        } else {
            taskLog("Entrei no ELSE");
            bean.setParticipantType(PhdParticipantBean.PhdParticipantType.EXTERNAL);
            taskLog(advisor.toString());
            taskLog(advisor.get("istAdvisor").toString());
            bean.setName(getProperty(advisor, "externalAdvisorName"));
            bean.setQualification(getProperty(advisor, "externalAdvisorDegree"));
            bean.setCategory(getProperty(advisor, "externalAdvisorCategory"));
            bean.setWorkLocation(getProperty(advisor, "externalAdvisorAffiliation"));
            bean.setInstitution(getProperty(advisor, "externalAdvisorInstitution"));
            bean.setAddress(getProperty(advisor, "externalAdvisorAddress"));
            bean.setEmail(getProperty(advisor, "externalAdvisorEmail"));
            bean.setPhone(getProperty(advisor, "externalAdvisorTelephone"));
        }
        PhdParticipant guiding = phdProcess.addGuiding(bean);
    }

    private String getProperty(final JsonObject jsonObject, final String key) {
        if (jsonObject.has(key)) {
            JsonElement jsonElement = jsonObject.get(key);
            return jsonObject.isJsonNull() ? null : jsonElement.toString();
        }
        throw new Error("Key not found for " + jsonObject.toString());
    }

    public void registrationFormalization(final RegistrationFormalizationBean bean, final Person responsible,
                                          final PhdIndividualProgramProcess phdProcess,
                                          final ExecutionYear executionYear) {

        LocalDate whenFormalizedRegistration = new LocalDate();
        phdProcess.setWhenFormalizedRegistration(whenFormalizedRegistration);
        phdProcess.setWhenStartedStudies(bean.getWhenStartedStudies());

        responsible.ensureOpenUserAccount();

        final DegreeCurricularPlan dcp = phdProcess.getCandidacyProcess().getPhdProgramLastActiveDegreeCurricularPlan();
        phdProcess.getCandidacyProcess().assertCandidacy(dcp, executionYear);
        phdProcess.getCandidacyProcess().assertRegistrationFormalizationAlerts();

        //é preciso ir ao último passo do flow, ver quando o CC homologou
        final LocalDate whenRatified = new LocalDate();
        phdProcess.getCandidacyProcess().setWhenRatified(whenRatified);

        PhdCandidacyProcessState candidacySet = phdProcess.getCandidacyProcess().getStatesSet().iterator().next();
        candidacySet.setStateDate(whenRatified.toDateTimeAtStartOfDay().minusHours(1));
        phdProcess.getCandidacyProcess().createState(PhdProgramCandidacyProcessState.PENDING_FOR_COORDINATOR_OPINION, responsible, "");
        phdProcess.getCandidacyProcess().createState(PhdProgramCandidacyProcessState.WAITING_FOR_SCIENTIFIC_COUNCIL_RATIFICATION, responsible, "");
        phdProcess.getCandidacyProcess().createState(PhdProgramCandidacyProcessState.RATIFIED_BY_SCIENTIFIC_COUNCIL, responsible, "");
        phdProcess.getCandidacyProcess().createState(PhdProgramCandidacyProcessState.CONCLUDED, responsible, "");
        phdProcess.createState(PhdIndividualProgramProcessState.WORK_DEVELOPMENT, responsible, "");

        phdProcess.getCandidacyProcess().assertRegistration(bean, dcp, executionYear);
    }
}
