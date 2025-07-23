package pt.ist.fenix.webapp.task.paperPusher.academic;

import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.smartFlow.domain.ActionNode;
import org.fenixedu.smartFlow.domain.Flow;
import org.fenixedu.smartFlow.domain.FlowNode;
import pt.ist.fenixframework.FenixFramework;

public class HackPhdFlow extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        final Flow flow = FenixFramework.getDomainObject("1697873586555284");
        final FlowNode last = flow.getLastFlowNode();
        final FlowNode prev = last.getPrev();
        final FlowNode secondPrev = prev.getPrev();
        final FlowNode thirdPrev = secondPrev.getPrev();
        last.delete();
        prev.delete();
        secondPrev.delete();
        final String nodeId = "step-advisor-accept-1";
        new FlowNode(thirdPrev, nodeId);
        final ActionNode node = (ActionNode) flow.nodeBy(nodeId);
        flow.moveQueue(node.queue);
    }

}