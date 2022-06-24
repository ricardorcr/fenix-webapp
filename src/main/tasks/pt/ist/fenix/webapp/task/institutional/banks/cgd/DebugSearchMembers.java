package pt.ist.fenix.webapp.task.institutional.banks.cgd;

import com.qubit.solution.fenixedu.integration.cgd.webservices.CgdIntegrationService;
import com.qubit.solution.fenixedu.integration.cgd.webservices.messages.member.SearchMemberInput;
import com.qubit.solution.fenixedu.integration.cgd.webservices.messages.member.SearchMemberOutput;
import org.fenixedu.bennu.scheduler.custom.ReadCustomTask;

import java.util.stream.Collector;
import java.util.stream.Collectors;

public class DebugSearchMembers extends ReadCustomTask {
    @Override
    public void runTask() throws Exception {
        final SearchMemberInput input = new SearchMemberInput();
        input.setDocumentID("230124674");
        input.setDocumentType(501);
        final SearchMemberOutput output = new CgdIntegrationService().searchMember(input);
        taskLog("%s = %s%n", output.getReplyCode(), output.getMemberInfo().stream()
                .map(info -> info.getName())
                .collect(Collectors.joining("; ")));
    }
}