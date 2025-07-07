package pt.ist.fenix.webapp.task.gep;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.Group;
import org.fenixedu.bennu.scheduler.custom.WriteCustomTask;
import org.fenixedu.drive.domain.AbstractFileNode;
import org.fenixedu.drive.domain.DirNode;
import org.fenixedu.drive.domain.FileNode;
import org.fenixedu.drive.domain.SharedFileNode;
import pt.ist.fenixframework.FenixFramework;

public class DistributeRADISTFiles extends WriteCustomTask {

    @Override
    public void runTask() throws Exception {
        final DirNode src = FenixFramework.getDomainObject("288553083203972");
        process(src);
    }

    private void process(final DirNode dir) {
        dir.getChildSet().stream()
                .filter(child -> child.isDir())
                .map(DirNode.class::cast)
                .forEach(this::process);
        dir.getChildSet().stream()
                .filter(child -> child.isFile())
                .map(FileNode.class::cast)
                .forEach(this::link);
    }

    private void link(final FileNode fileNode) {
        final String name = fileNode.getName();
        final String[] nameParts = name.split("_");//mc_istxxxxx_2022_2024.xlsx
        final String username = nameParts[1];
        taskLog(name);
        final User user = User.findByUsername(username);
        final DirNode repo = user.getFileRepository();
        final DirNode officialDocs = getOrCreateDir(repo, "Documentos Oficiais", "documentos-oficiais");
        final DirNode radistDocs = getOrCreateDir(officialDocs, "RADDIST", "raddist");
        final DirNode radist2025Docs = getOrCreateDir(radistDocs, "2022/2024", "20222024");
        radist2025Docs.getChildSet().forEach(AbstractFileNode::delete);
        SharedFileNode sharedFileNode = new SharedFileNode(fileNode);
        radist2025Docs.addChild(sharedFileNode);
        fileNode.setReadGroup(Group.users(user));
    }

    private DirNode getOrCreateDir(final DirNode dirNode, final String documentosOficiais, final String slug) {
        return dirNode.getChildSet().stream()
                .filter(child -> child.isDir())
                .map(DirNode.class::cast)
                .filter(dir -> dir.getSlug().equals(slug))
                .findAny()
                .orElseGet(() -> {
                    final DirNode result = new DirNode(dirNode, documentosOficiais);
                    result.setWriteGroup(Group.users(User.findByUsername("fenix")));
                    return result;
                });
    }

}