package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class SendWelcomeSMSFromDGesFile extends CustomTask {

    private final String DGES_FILE_A = "/afs/ist.utl.pt/ciist/fenix/fenix015/Colocados_por_Curso_dados_fenix_alameda_2_Fase.txt";
    private final String DGES_FILE_T = "/afs/ist.utl.pt/ciist/fenix/fenix015/Colocados_por_Curso_dados_fenix_taguspark_2_Fase.txt";
    protected static final String ALAMEDA_UNIVERSITY = "A";
    protected static final String TAGUS_UNIVERSITY = "T";
    private final String LINK = "https://ist.pt/matriculas";

    int smsCount = 0;
    int studentCandidacyCount = 0;

    @Override
    public void runTask() throws Exception {
//        final ExecutionYear executionYear = ExecutionYear.readCurrentExecutionYear();
//        final Space alameda = Space.getTopLevelSpaces().stream().filter(s -> s.getName().equals("Alameda")).findAny().orElse(null);
//        final Space tagus = Space.getTopLevelSpaces().stream().filter(s -> s.getName().equals("Taguspark")).findAny().orElse(null);
//
//        process(executionYear, alameda, parseDgesFile(Files.readAllBytes(new File(DGES_FILE_A).toPath()), ALAMEDA_UNIVERSITY, EntryPhase.FIRST_PHASE));
//        process(executionYear, tagus, parseDgesFile(Files.readAllBytes(new File(DGES_FILE_T).toPath()), TAGUS_UNIVERSITY, EntryPhase.FIRST_PHASE));
//
//        taskLog("%nSent a total of %s sms's to %s candidates.", smsCount, studentCandidacyCount);
//    }
//
//    private void process(final ExecutionYear executionYear, final Space space, final List<DegreeCandidateDTO> degreeCandidateDTOs) {
//        degreeCandidateDTOs.forEach(dto -> {
//            final String degreeCode = dto.getDegreeCode();
//            final ExecutionDegree executionDegree = ExecutionDegree.readByDegreeCodeAndExecutionYearAndCampus(degreeCode, executionYear, space);
//            final String number = dto.getPhoneNumber();
//            if (number == null || number.isEmpty()) {
//                taskLog("Unable to send sms for: %s - no number%n", dto.getName());
//            } else if (executionDegree == null) {
//                taskLog("Unable to send sms for: %s - no execution degree: %s%n", dto.getName(), degreeCode);
//            } else {
//                sendSMSWithDegreeCode(number, executionDegree.getDegree().getSigla());
//            }
//        });
//    }
//
//    private void sendSMSWithDegreeCode(final String number, final String degree) {
//        studentCandidacyCount++;
//
//        final StringBuilder message = new StringBuilder();
//        message.append("Parabéns! ");
//        message.append("Foste colocado no curso " + degree + " do Técnico. ");
//        message.append("Datas e documentos acerca da matrícula podem ser acedidos em ");
//        message.append(LINK);
//
//        if (PhoneUtil.isMobileNumber(number)) {
//            sendSMS(number.replace(" ", ""), normalize(message.toString()));
//        } else {
//            taskLog("Unable to send sms to invalid number: %s%n", number);
//        }
//    }
//
//    private String normalize(final String string) {
//        return StringNormalizer.normalizePreservingCapitalizedLetters(string);
//    }
//
//    private void sendSMS(String number, final String message) {
//        number = number.startsWith("+") ? number : "+351" + number;
//        smsCount++;
//        taskLog("%nSending sms to number %s. SMS lenght: %s%n", number, message.length());
//        taskLog("%s%n", message);
//
//        try {
//            final boolean ok = PhoneValidationUtils.getInstance().sendSMSMessage(number, message);
//            taskLog("Sent SMS via twilio to %s : %s%n", number, ok);
//        } catch (final Throwable t) {
//            taskLog("Faild send sms with exception: %s to number %s%n", t.getMessage(), number);
//            t.printStackTrace();
//        }
//    }
//
//    protected List<DegreeCandidateDTO> parseDgesFile(byte[] contents, String university, EntryPhase entryPhase) {
//
//        final List<DegreeCandidateDTO> result = new ArrayList<DegreeCandidateDTO>();
//        String[] lines = readContent(contents);
//        for (String dataLine : lines) {
//            DegreeCandidateDTO dto = new DegreeCandidateDTO();
//            if (dto.fillWithFileLineData(dataLine)) {
//                result.add(dto);
//            }
//        }
//        setConstantFields(university, entryPhase, result);
//        return result;
//
//    }
//
//    private void setConstantFields(String university, EntryPhase entryPhase, final Collection<DegreeCandidateDTO> result) {
//        for (final DegreeCandidateDTO degreeCandidateDTO : result) {
//            degreeCandidateDTO.setIstUniversity(university);
//            degreeCandidateDTO.setEntryPhase(entryPhase);
//        }
//    }
//
//    public static String[] readContent(byte[] contents) {
//        try {
//            String fileContents = new String(contents, "UTF-8");
//            return fileContents.split("\n");
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException(e);
//        }
    }

}