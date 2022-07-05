package pt.ist.fenix.webapp.task.institutional.banks.cgd;

import com.qubit.solution.fenixedu.integration.cgd.services.form43.CgdForm43Sender;
import com.qubit.solution.fenixedu.integration.cgd.webservices.CgdIntegrationService;
import com.qubit.solution.fenixedu.integration.cgd.webservices.messages.member.SearchMemberInput;
import com.qubit.solution.fenixedu.integration.cgd.webservices.messages.member.SearchMemberOutput;
import org.apache.commons.lang.BooleanUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.student.Registration;
import org.fenixedu.academic.domain.student.Student;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.papyrus.service.PapyrusPdfRendererService;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import org.joda.time.DateTime;
import pt.ist.fenixedu.integration.domain.cgd.CgdCard;
import pt.ist.fenixedu.integration.ui.spring.service.RegistrationDeclarationForBanksService;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.papyrus.PapyrusClient;
import pt.ist.papyrus.PapyrusConfiguration;
import pt.ist.papyrus.PapyrusSettings;
import pt.ist.registration.process.ui.service.RegistrationDeclarationDataProvider;
import services.caixaiu.cgd.wingman.iesservice.IIESService;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public class DebugSearchMembers extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dumpTreshold", "9999999999999999999999999999999999999");

/*
        final SearchMemberInput input = new SearchMemberInput();
        input.setDocumentID("230124674");
        input.setDocumentType(501);
        final SearchMemberOutput output = new CgdIntegrationService().searchMember(input);
        taskLog("%s = %s%n", output.getReplyCode(), output.getMemberInfo().stream()
                .map(info -> info.getName())
                .collect(Collectors.joining("; ")));
        final User user = User.findByUsername("ist423218");
 */
        final CgdCard cgdCard = FenixFramework.getDomainObject("851898173307065");
/*
        final RegistrationDeclarationForBanksService rservice = new RegistrationDeclarationForBanksService(
                new RegistrationDeclarationDataProvider(),
                new PapyrusPdfRendererService(new PapyrusClient(PapyrusConfiguration.getConfiguration().papyrusUrl(),
                        PapyrusConfiguration.getConfiguration().papyrusToken()), PapyrusSettings.newBuilder().build()));
        final SendCgdCardService service = new SendCgdCardService(rservice);
        service.sendCgdCard(cgdCard);
 */
        xpto(cgdCard);
    }

    public void xpto(final CgdCard cgdCard) {
        if (cgdCard == null) {
            taskLog("CGD: Não existe cartão para este pedido.");
            return;
        }
        final Person person = cgdCard.getUser().getPerson();
        final String username = cgdCard.getUser().getUsername();
        if (BooleanUtils.isTrue(cgdCard.getAllowSendBankDetails())) {
            if (person != null) {
                final Student student = person.getStudent();
                if (student != null) {
                    for (final Registration registration : student.getRegistrationsSet()) {
                        if (registration.isActive()) {
                            CgdForm43Sender sender = new CgdForm43Sender();
                            final IIESService service = sender.getClient();
                            try {
                                final Method method = sender.getClass().getDeclaredMethod("getService");
                                method.setAccessible(true);
                                final BindingProvider provider = (BindingProvider) method.invoke(sender);

                                final org.apache.cxf.endpoint.Client client = ClientProxy.getClient(provider);
                                LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
                                loggingInInterceptor.setPrettyLogging(true);
                                LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
                                loggingOutInterceptor.setPrettyLogging(true);

                                client.getInInterceptors().add(loggingInInterceptor);
                                client.getOutInterceptors().add(loggingOutInterceptor);

                            } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                                throw new Error(e);
                            }
                            boolean form = sender.sendForm43For(registration);

                            final RegistrationDeclarationForBanksService rservice = new RegistrationDeclarationForBanksService(
                                    new RegistrationDeclarationDataProvider(),
                                    new PapyrusPdfRendererService(new PapyrusClient(PapyrusConfiguration.getConfiguration().papyrusUrl(),
                                            PapyrusConfiguration.getConfiguration().papyrusToken()), PapyrusSettings.newBuilder().build()));

                            boolean attachment = sender.uploadFormAttachment(registration, rservice
                                    .getRegistrationDeclarationFileForBanks(registration));
                            taskLog("Sent Form43 ({}) and registration declaration file ({}) for registration {}%n",
                                    form, attachment, registration.getExternalId() );
                            if (form && attachment) {
                                FenixFramework.atomic(() -> cgdCard.setSuccessfulSentData(new DateTime()));
                                taskLog(String.format("CGD: Comunicação efectuada à CGD com sucesso para o utilizador %s%n", username));
                                return;
                            } else {
                                taskLog(String.format("CGD: Comunicação falhou para o utilizador %s. Contactar a CGD.%n", username));
                                return;
                            }
                        }
                    }
                    taskLog(String.format("CGD: Não existe uma matrícula activa para o aluno %s%n", username));
                    return;
                }
                taskLog(String.format("CGD: Utilizador %s não é aluno%n", username));
                return;
            } else {
                taskLog(String.format("CGD: Utilizador %s não tem pessoa activa%n", username));
                return;
            }
        }
        taskLog(String.format("CGD: %s - É necessário autorização a cedência de dados à CGD para efeitos de abertura de conta%n", username));
    }

}