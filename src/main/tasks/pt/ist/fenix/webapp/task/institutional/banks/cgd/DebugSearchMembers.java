package pt.ist.fenix.webapp.task.institutional.banks.cgd;

import com.qubit.solution.fenixedu.integration.cgd.webservices.CgdIntegrationService;
import com.qubit.solution.fenixedu.integration.cgd.webservices.messages.member.SearchMemberInput;
import com.qubit.solution.fenixedu.integration.cgd.webservices.messages.member.SearchMemberOutput;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.papyrus.service.PapyrusPdfRendererService;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;
import pt.ist.fenixedu.integration.domain.cgd.CgdCard;
import pt.ist.fenixedu.integration.ui.spring.service.RegistrationDeclarationForBanksService;
import pt.ist.fenixedu.integration.ui.spring.service.SendCgdCardService;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.papyrus.PapyrusClient;
import pt.ist.papyrus.PapyrusConfiguration;
import pt.ist.papyrus.PapyrusSettings;
import pt.ist.registration.process.ui.service.RegistrationDeclarationDataProvider;

import java.util.stream.Collectors;

public class DebugSearchMembers extends ReadCustomTask {

    @Override
    public void runTask() throws Exception {
        System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
        System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dumpTreshold", "999999");

        final SearchMemberInput input = new SearchMemberInput();
        input.setDocumentID("230124674");
        input.setDocumentType(501);
        final SearchMemberOutput output = new CgdIntegrationService().searchMember(input);
        taskLog("%s = %s%n", output.getReplyCode(), output.getMemberInfo().stream()
                .map(info -> info.getName())
                .collect(Collectors.joining("; ")));
        final User user = User.findByUsername("ist423218");
        final CgdCard cgdCard = FenixFramework.getDomainObject("851898173307065");
        final RegistrationDeclarationForBanksService rservice = new RegistrationDeclarationForBanksService(
                new RegistrationDeclarationDataProvider(),
                new PapyrusPdfRendererService(new PapyrusClient(PapyrusConfiguration.getConfiguration().papyrusUrl(),
                        PapyrusConfiguration.getConfiguration().papyrusToken()), PapyrusSettings.newBuilder().build()));
        final SendCgdCardService service = new SendCgdCardService(rservice);
        service.sendCgdCard(cgdCard);
    }

}