package pt.ist.fenix.webapp.task.accounting;

import org.apache.commons.lang3.tuple.MutablePair;
import org.fenixedu.academic.domain.accounting.IBAN;
import org.fenixedu.academic.domain.accounting.IBANGroup;
import org.fenixedu.academic.domain.accounting.IBANPayment;
import org.fenixedu.academic.util.Money;
import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.messaging.core.domain.Message;
import org.fenixedu.messaging.core.domain.MessagingSystem;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class ImportIBANsSettlementInfoLocal extends ReadCustomTask {

    final String INPUT_DIRECTORY = "/home/rcro/Documents/fenix/pagamentos/ibans/new";
    final String OUTPUT_DIRECTORY = "/home/rcro/Documents/fenix/pagamentos/ibans/processed";

    @Override
    public void runTask() throws Exception {
        final File input = new File(INPUT_DIRECTORY);

        final File output = new File(OUTPUT_DIRECTORY);
        if (!output.exists()) {
            output.mkdirs();
        }

        if (input.exists()) {
            for (final File file : Objects.requireNonNull(input.listFiles())) {
                if (file.getName().endsWith(".txt")) {
                    List<String> lines = Files.readAllLines(file.toPath());
                    processFile(lines, file.getName());
                    final File dest = new File(output, file.getName());
                    Files.move(file.toPath(), dest.toPath(),
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void processFile(final List<String> content, final String fileName) {
        FenixFramework.atomic(() -> {
            try {
                final MutablePair<String, List<String>> errors = importIBANs(content, fileName);
                if (!errors.getRight().isEmpty() || errors.getLeft() != null) {
                    sendEmail(errors);
                }
            } catch (Exception e) {
                throw new Error(e);
            }
        });
    }


    private void sendEmail(final MutablePair<String, List<String>> errors) throws IOException, AddressException {
        final String emailAddressesFilePath = GiafInvoiceConfiguration.getConfiguration().clientSapLogErrorsMails();
        final String bccEmailAddressesFilePath = GiafInvoiceConfiguration.getConfiguration().clientSapLogErrorsMailsBcc();
        final String emails = Files.readAllLines(new File(emailAddressesFilePath).toPath()).iterator().next();
        final String bccEmails = Files.readAllLines(new File(bccEmailAddressesFilePath).toPath()).iterator().next();
        final Stream<String> emailsStream = Arrays.stream(InternetAddress.parse(emails)).map(InternetAddress::getAddress);
        final Stream<String> bccEmailsStream = Arrays.stream(InternetAddress.parse(bccEmails)).map(InternetAddress::getAddress);
        StringBuilder body = new StringBuilder();
        if (!errors.getLeft().isEmpty()) {
            body.append(errors.getLeft());
            body.append("\n\n");
        }
        if (!errors.getRight().isEmpty()) {
            body.append("As seguintes referências IBAN foram recebidas no banco mas não foram geradas pelo Fénix:");
            body.append("\n\n");
            for (String reference : errors.getRight()) {
                body.append(reference);
                body.append("\n");
            }
        }
        Message.from(MessagingSystem.systemSender())
                .subject("Erros na importação de IBANs dedicados")
                .singleBcc(bccEmailsStream)
                .singleTos(emailsStream)
                .textBody(body.toString())
                .send();
    }

    @Atomic
    private MutablePair<String, List<String>> importIBANs(final List<String> allLines, final String fileName) {
        IBANGroup ibanGroup = null;
        final Iterator<String> iter = allLines.iterator();
        String accountLine = null;
        MutablePair<String, List<String>> errors = new MutablePair<>();
        List<String> notFoundReferences = new ArrayList<>();
        final Map<LocalDate, Integer[]> dateCount = new HashMap<>();
        while (iter.hasNext()) {
            final String line = iter.next();
            if (line.startsWith(":25:")) {
                accountLine = line;
                final String[] split = line.split(":25:");
                final Optional<IBANGroup> optional = Bennu.getInstance().getIBANGroupSet().stream()
                        .filter(group -> group.getAccount() == Long.parseLong(split[1]))
                        .findAny();
                if (optional.isPresent()) {
                    ibanGroup = optional.get();
                } else {
                    errors.setLeft("Erro ao processar o ficheiro " + fileName + ": a conta " + split[1] + " não é a conta esperada");
                }
                continue;
            }
            if (line.startsWith(":61:")) {
                final String secondLine = iter.next();
                if (!secondLine.startsWith(":86:TRF REF:")) {
                    continue;
                }
                final int year = Integer.parseInt("20" + line.substring(4, 6));
                LocalDate settlementDate = new LocalDate(year, Integer.parseInt(line.substring(6, 8)), Integer.parseInt(line.substring(8, 10)));
                LocalDate operationDate = new LocalDate(year, Integer.parseInt(line.substring(10, 12)), Integer.parseInt(line.substring(12, 14)));
                final int indexOf = line.indexOf("NMSCNONREF");
                final Money amount = new Money(line.substring(15, indexOf).replace(",", "."));

                final Integer counter = dateCount.computeIfAbsent(settlementDate, v -> new Integer[]{0})[0]++;

                final String[] parts = secondLine.split(":86:TRF REF:");
                final String reference = parts[1].trim().split(" ")[0];

                StringBuilder settlement = new StringBuilder(String.valueOf(counter)).append(" - ").append(accountLine);
                settlement.append("\n").append(line);
                settlement.append("\n").append(secondLine);

                final IBANPayment lookup = IBANPayment.lookup(settlement.toString());
                if (lookup != null) {
                    //Já foi tratado
                    continue;
                }

                final IBAN iban = ibanGroup.lookup(reference);
                if (iban == null) {
                    if (reference.startsWith("0")) {
                        //IBAN dedicado para esta conta fora do Fénix?
                        notFoundReferences.add(reference + " - " + operationDate.toString());
                    }
                    continue;
                }
                new IBANPayment(iban, operationDate, settlementDate, amount, settlement.toString());
            }
        }
        errors.setRight(notFoundReferences);
        return errors;
    }
}
