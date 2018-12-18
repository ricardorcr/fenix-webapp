package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;

public class ManualGenerateSibsPaymentFileSpecialSeason extends CustomTask {

    private static final String LINE_REGISTER_TYPE = "1";
    private static final String LINE_PROCESSING_CODE = "80";
    private static int DECIMAL_PLACES_FACTOR = 100;
    private static final int WHITE_SPACES_IN_LINE = 2;
    private static final int AMOUNT_LENGTH = 10;
    private static final String DATE_FORMAT = "yyyyMMdd";
    private static final String NUMBER_FILLER = "0";
    private static final String LINE_TERMINATOR = "\r\n";
    private static final String FOOTER_REGISTER_TYPE = "9";
    private static final int NUMBER_OF_LINES_DESCRIPTOR_LENGTH = 8;
    public static final int WHITE_SPACES_IN_FOOTER = 41;

    @Override
    public void runTask() throws Exception {

//        final SIBSOutgoingPaymentFile lastSuccessfulSent = SIBSOutgoingPaymentFile.readLastSuccessfulSentPaymentFile();
//        final DateTime lastOutgoingPaymentFileSent =
//                lastSuccessfulSent != null ? lastSuccessfulSent.getSuccessfulSentDate() : new DateTime();
//        final SIBSOutgoingPaymentFile sibsOutgoingPaymentFile = new SIBSOutgoingPaymentFile(lastOutgoingPaymentFileSent);
//
//        byte[] before = sibsOutgoingPaymentFile.getContent();
//        output("before.txt", before);
//
//        final StringBuilder builder = new StringBuilder();
//        final String[] lines = new String(before).split(LINE_TERMINATOR);
//        for (int i = 0; i < lines.length - 1; i++) {
//            builder.append(lines[i]);
//            builder.append(LINE_TERMINATOR);
//        }
//
//        AtomicInteger count = new AtomicInteger(0);
//        Bennu.getInstance().getStudentsSet().stream().map(Student::getPerson).filter(Objects::nonNull)
//                .flatMap(p -> p.getEventsSet().stream()).filter(e -> e instanceof SpecialSeasonEnrolmentEvent)
//                .filter(Event::isOpen).forEach(e -> addLine(e, builder, count));
//
//        builder.append(FOOTER_REGISTER_TYPE);
//        builder.append(StringUtils.leftPad(String.valueOf(lines.length - 2 + count.get()), NUMBER_OF_LINES_DESCRIPTOR_LENGTH,
//                NUMBER_FILLER));
//        builder.append(StringUtils.leftPad("", WHITE_SPACES_IN_FOOTER));
//        builder.append(LINE_TERMINATOR);
//
//        byte[] after = builder.toString().getBytes();
//        output(sibsOutgoingPaymentFile.getFilename(), after);
//
//        final Method method = GenericFile.class.getDeclaredMethod("setContent", new Class[] { byte[].class });
//        method.setAccessible(true);
//        method.invoke(sibsOutgoingPaymentFile, new Object[] { after });
    }

//    private void addLine(Event event, StringBuilder builder, AtomicInteger count) {
//        AccountingEventPaymentCode paymentCode = event.getAllPaymentCodes().iterator().next();
//
//        builder.append(LINE_REGISTER_TYPE);
//        builder.append(LINE_PROCESSING_CODE);
//        builder.append(paymentCode.getCode());
//        builder.append(paymentCode.getEndDate().toString(DATE_FORMAT));
//        builder.append(leftPadAmount(paymentCode.getMinAmount()));
//        builder.append(paymentCode.getStartDate().toString(DATE_FORMAT));
//        builder.append(leftPadAmount(paymentCode.getMaxAmount()));
//        builder.append(StringUtils.leftPad("", WHITE_SPACES_IN_LINE));
//        builder.append(LINE_TERMINATOR);
//
//        count.incrementAndGet();
//    }
//
//    private String leftPadAmount(final Money amount) {
//        return StringUtils.leftPad(String.valueOf(amount.multiply(BigDecimal.valueOf(DECIMAL_PLACES_FACTOR)).longValue()),
//                AMOUNT_LENGTH, NUMBER_FILLER);
//    }

}