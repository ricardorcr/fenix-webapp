package pt.ist.fenix.webapp;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.fenixedu.bennu.io.domain.GenericFile;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.messaging.core.domain.Message;
import org.fenixedu.messaging.core.domain.MessagingSystem;
import org.fenixedu.messaging.core.domain.Sender;
import org.fenixedu.messaging.emaildispatch.EmailDispatchConfiguration;
import org.fenixedu.messaging.emaildispatch.domain.EmailBlacklist;
import org.fenixedu.messaging.emaildispatch.domain.LocalEmailMessageDispatchReport;
import org.joda.time.DateTime;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

public class HackEmails extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        MessagingSystem.getInstance().getUnfinishedReportsSet().forEach(report -> {
            try {
                deliver(report);
            } catch (final Exception ex) {
                taskLog("Failed to send: %s %s%n", report.getExternalId(), report.getMessage().getExternalId());
                final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                final PrintStream stream = new PrintStream(outputStream);
                ex.printStackTrace(stream);
                taskLog("%s%n", new String(outputStream.toByteArray()));
            }
        });
    }

    public void deliver(final LocalEmailMessageDispatchReport report) throws MessagingException {
        try {
            MimeMessage message = mimeMessage(report);
            taskLog("message: " + message);
            taskLog("message size: " + message.getSize());
            taskLog("message description: " + message.getDescription());
            for (final Address address : message.getAllRecipients()) {
                taskLog("   Address: [%s%n]", address);
            }
            taskLog("RecipientCount: %s%n", message.getAllRecipients().length);
            taskLog("Message: %s%n", message);
            taskLog("Transport: %s%n", Transport.class.getName());
            Transport.send(message);
            report.setDeliveredCount(report.getDeliveredCount() + message.getAllRecipients().length);
        }
        catch (SendFailedException e) {
            if (e.getValidSentAddresses() != null) {
                report.setDeliveredCount(report.getDeliveredCount() + e.getValidSentAddresses().length);
            }
            if (e.getInvalidAddresses() != null) {
                report.setFailedCount(report.getFailedCount() + e.getInvalidAddresses().length);
                for (Address failed : e.getInvalidAddresses()) {
                    EmailBlacklist.getInstance().addFailedAddress(failed.toString());
                }
            }
            if (e.getValidUnsentAddresses() != null) {
                if (true) throw new Error("not implemented");
                HashSet<InternetAddress> invalidAddresses = null;//getInvalidsFromExceptionChain(e.getNextException());
                report.setFailedCount(report.getFailedCount() + invalidAddresses.size());
                invalidAddresses.forEach(failed -> EmailBlacklist.getInstance().addFailedAddress(failed.toString()));

                Address[] onlyValidAddress = Sets.difference(Sets.newHashSet(e.getValidUnsentAddresses()), invalidAddresses)
                        .toArray(new Address[0]);
                throw new Error("resend(onlyValidAddress);");
            }
        }
        throw new Error("delete();");
    }

    private static final int MAX_RECIPIENTS = EmailDispatchConfiguration.getConfiguration().mailSenderMaxRecipients();
    private static final String MIME_MESSAGE_ID_SUFFIX = EmailDispatchConfiguration.getConfiguration().mailMimeMessageIdSuffix();

    private static Session SESSION = null;

    private static synchronized Session session() {
        final Properties properties = new Properties();
        final EmailDispatchConfiguration.ConfigurationProperties conf = EmailDispatchConfiguration.getConfiguration();
        properties.put("mail.smtp.host", conf.mailSmtpHost());
        properties.put("mail.smtp.name", conf.mailSmtpName());
        properties.put("mail.smtp.port", conf.mailSmtpPort());
        properties.put("mailSender.max.recipients", conf.mailSenderMaxRecipients());
        SESSION = Session.getDefaultInstance(properties, null);
        return SESSION;
    }

    private InternetAddress getFrom(final LocalEmailMessageDispatchReport report) throws MessagingException {
        final Sender sender = report.getMessage().getSender();
        final String name = sender.getName();
        final String address = sender.getAddress();
        try {
            return new InternetAddress(address, name, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Unsupported utf-8 encoding when building from address", e);
        }
    }

    private static String getContent(LocalizedString ls, Locale l) {
        if (ls != null) {
            String s = ls.getContent(l);
            if (s == null) {
                return ls.getContent();
            }
            return s;
        }
        return null;
    }
    protected MimeMessage mimeMessage(final LocalEmailMessageDispatchReport report) throws MessagingException {
        final Message message = report.getMessage();
        final Locale locale = message.getPreferredLocale();
        final String[] languages = {locale.toLanguageTag()};
        MimeMessage mimeMessage = new MimeMessage(session()) {
            private String fenixMessageId = null;

            @Override
            public String getMessageID() throws MessagingException {
                if (fenixMessageId == null) {
                    fenixMessageId = report.getExternalId() + "." + new DateTime().getMillis() + "@" + MIME_MESSAGE_ID_SUFFIX;
                }
                return fenixMessageId;
            }

            @Override
            protected void updateMessageID() throws MessagingException {
                setHeader("Message-ID", getMessageID());
                setSentDate(message.getCreated().toDate());
            }

        };

        mimeMessage.setFrom(getFrom(report));
        mimeMessage.setContentLanguage(languages);
        mimeMessage.setSubject(getContent(message.getSubject(), locale));

        final String replyTo = message.getReplyTo();
        if (!Strings.isNullOrEmpty(replyTo)) {
            // Use parse since there may be multiple reply tos specified
            Address[] replyTos = InternetAddress.parse(replyTo);
            mimeMessage.setReplyTo(replyTos);
        }

        // Main Message MimeMultipart
        final MimeMultipart mimeMultipart = new MimeMultipart("mixed");

        // MimeMultipart for html+text content
        final MimeMultipart htmlAndTextMultipart = new MimeMultipart("alternative");

        // Should be ordered "plainest to richest" (first: text/plain | second: text/html) to display properly in email clients
        final String textBody = getContent(message.getTextBody(), locale);
        if (!Strings.isNullOrEmpty(textBody)) {
            final BodyPart bodyPart = new MimeBodyPart();
            bodyPart.setContent(textBody, "text/plain; charset=utf-8");
            htmlAndTextMultipart.addBodyPart(bodyPart);
        }

        final String htmlBody = getContent(message.getHtmlBody(), locale);
        if (!Strings.isNullOrEmpty(htmlBody)) {
            final BodyPart bodyPart = new MimeBodyPart();
            bodyPart.setContent(htmlBody, "text/html; charset=utf-8");
            htmlAndTextMultipart.addBodyPart(bodyPart);
        }

        // Store HTML+text Multipart inside a BodyPart to add to main Message MimeMultipart
        final MimeBodyPart htmlAndTextBodypart = new MimeBodyPart();
        htmlAndTextBodypart.setContent(htmlAndTextMultipart);
        mimeMultipart.addBodyPart(htmlAndTextBodypart);

        for (final GenericFile file : message.getFileSet()) {
            final MimeBodyPart bodyPart = new MimeBodyPart();
            bodyPart.setDataHandler(new DataHandler(new DataSource() {
                @Override public InputStream getInputStream() {
                    return file.getStream();
                }

                @Override public OutputStream getOutputStream() {
                    throw new UnsupportedOperationException();
                }

                @Override public String getContentType() {
                    return file.getContentType();
                }

                @Override public String getName() {
                    return file.getFilename();
                }
            }));
            bodyPart.setFileName(file.getFilename());
            mimeMultipart.addBodyPart(bodyPart);
        }

        mimeMessage.setContent(mimeMultipart);

        /*
        String addresses = getToAddresses();
        if (addresses != null) {
            mimeMessage.addRecipients(MimeMessage.RecipientType.TO, addresses);
        }
        addresses = getCcAddresses();
        if (addresses != null) {
            mimeMessage.addRecipients(MimeMessage.RecipientType.CC, addresses);
        }
        addresses = getBccAddresses();
        if (addresses != null) {
            mimeMessage.addRecipients(MimeMessage.RecipientType.BCC, addresses);
        }
        */

        return mimeMessage;
    }
}