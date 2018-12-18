package pt.ist.fenix.webapp;

import org.fenixedu.academic.domain.ExecutionYear;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class ProcessCandidaciesToAdmissions extends CustomTask {

    ExecutionYear executionYear = null;
    
    @Override
    public void runTask() throws Exception {

//        executionYear = ExecutionYear.readExecutionYearByName("2020/2021");
//        Locale PT = Locale.forLanguageTag("pt");
//        Locale EN = Locale.forLanguageTag("en");
//        LocalizedString alameda = new LocalizedString(PT, "Candidaturas Alameda").with(EN, "Alameda Applications");
//        LocalizedString tagus = new LocalizedString(PT, "Candidaturas Tagus").with(EN, "Tagus Applications");
//        AdmissionProcess admissionProcessAlameda = new AdmissionProcess(alameda, new DateTime().minusWeeks(1), new DateTime().plusWeeks(1), "");
//        AdmissionProcess admissionProcessTagus = new AdmissionProcess(tagus, new DateTime().minusWeeks(1), new DateTime().plusWeeks(1), "");;
//        
//        Stream<CandidacyProcess> candidacyProcesses = getCandidacyProcesses();
//        Stream<IndividualCandidacyProcess> stream = candidacyProcesses
//                .flatMap(cp -> cp.getChildProcessesSet().stream())
//                .map(process -> (IndividualCandidacyProcess)process);
//        
//        processCandidacies(stream, admissionProcessAlameda, admissionProcessTagus);
//    }
//
//    private void processCandidacies(Stream<IndividualCandidacyProcess> candidacyProcesses, AdmissionProcess admissionProcessAlameda, AdmissionProcess admissionProcessTagus) {
//        candidacyProcesses
//            .filter(process -> process.getCandidacy().getState().equals(IndividualCandidacyState.ACCEPTED))
//            .forEach(individualProcess -> {
//                if (!isExternal(individualProcess)) {
//                    //enviar mail para aluno IST
//                    sendInternalEmail(individualProcess.getCandidacy());
//                } else {
//                    //enviar mail candidato para se registar admission
//                    sendExternalEmail(individualProcess.getCandidacy());
//                    createApplication(individualProcess, admissionProcessAlameda, admissionProcessTagus);
//                }
//            });   
//    }
//
//    private void sendExternalEmail(IndividualCandidacy candidacy) {
//        if (isAlamedaCampus(candidacy)) {
//            sendMail(candidacy, "candidacy.external.alameda.accepted.email"); 
//        } else {
//            sendMail(candidacy, "candidacy.external.tagus.accepted.email"); 
//        }
//    }
//
//    private void sendInternalEmail(IndividualCandidacy candidacy) {
//           sendMail(candidacy, "candidacy.internal.accepted.email");
//    }
//
//    private void sendMail(IndividualCandidacy candidacy, String template) {
//        final Message message = Message.fromSystem()
//                .singleTos(candidacy.getPersonalDetails().getEmail())
//                .template(template)
//                .parameter("name", candidacy.getPersonalDetails().getPerson().getName())
//                .parameter("gender", candidacy.getPersonalDetails().getPerson().getGender().name())
//                .and()
//                .wrapped().send();     
//    }
//    
//    private void createApplication(IndividualCandidacyProcess individualProcess, AdmissionProcess admissionProcessAlameda, AdmissionProcess admissionProcessTagus) {
//        Candidate candidate = getCandidate(individualProcess.getPersonalDetails());
//        if (candidate != null && !hasApplication(candidate.getIdentity(), individualProcess)) {            
//            individualProcess.getCandidacy().getAllDegrees()
//                .forEach(degree -> {
//                    if (degree.getCampus(executionYear).stream().anyMatch(c -> c.getName().contains("Alameda"))) {                        
//                        createApplicationFor(admissionProcessAlameda, candidate, degree, individualProcess.getCandidacy());   
//                    } else {
//                        createApplicationFor(admissionProcessTagus, candidate, degree, individualProcess.getCandidacy());
//                    }
//                });
//        }       
//    }
//    
//    private boolean isAlamedaCampus(IndividualCandidacy candidacy) {
//        return candidacy.getAllDegrees().stream()
//            .anyMatch(degree -> degree.getCampus(executionYear).stream().anyMatch(c -> c.getName().contains("Alameda")));
//    }
//
//    private boolean hasApplication(Identity identity, IndividualCandidacyProcess individualProcess) {
//        return identity.getCandidateSet().stream()                
//                .flatMap(c -> c.getApplicationSet().stream())
//                .map(app -> app.get("candidacyId"))
//                .anyMatch(id -> individualProcess.getCandidacy().getExternalId().equals(id));
//    }
//
//    public boolean isCandidacyImported(IndividualCandidacyProcess process, AdmissionProcess admissionProcess) {
//        return admissionProcess.getApplicationSet().stream()
//                .anyMatch(app -> process.getCandidacy().getExternalId().equals(app.get("candidacyId")));
//    }
//    
//    private Candidate getCandidate(IndividualCandidacyPersonalDetails personalDetails) {
//        Identity identity = personalDetails.getPerson().getIdentity();
//        Candidate candidate = null;
//        if (identity != null) {
//            candidate = identity.getCandidateSet().stream()
//                .findAny().orElse(null);
//        }
//        return candidate;        
//    }
//
//    private void createApplicationFor(final AdmissionProcess admissionProcess, final Candidate candidate, Degree degree, IndividualCandidacy candidacy) {
//        final JsonObject data = new JsonObject();
//        data.addProperty("type", "Registration");
//        data.addProperty("title", degree.getPresentationName());
//        data.addProperty("degreeId", degree.getExternalId());
//        data.addProperty("candidacyId", candidacy.getExternalId());
//        data.addProperty("candidacyType", IndividualCandidacy.class.getName());
//        new Application(candidate, admissionProcess, data.toString());
//    }
//
//    private boolean isExternal(IndividualCandidacyProcess candidacy) {
//        return candidacy.getPersonalDetails().getPerson().getStudent() == null;        
//    }
//
//    private Stream<CandidacyProcess> getCandidacyProcesses() {
//        return Bennu.getInstance().getProcessesSet().stream()
//                .filter(CandidacyProcess.class::isInstance)
//                .filter(p -> !(p instanceof MobilityApplicationProcess) && !(p instanceof DegreeTransferCandidacyProcess))
//                .map(p -> (CandidacyProcess) p)
//                .filter(cp -> cp.getCandidacyExecutionInterval() == executionYear);
    }
}
