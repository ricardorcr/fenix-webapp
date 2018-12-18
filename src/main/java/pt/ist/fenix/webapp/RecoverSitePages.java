package pt.ist.fenix.webapp;

import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.cms.domain.Post;
import org.fenixedu.cms.domain.component.StaticPost;
import pt.ist.fenixframework.FenixFramework;

public class RecoverSitePages extends CustomTask {

    @Override
    public void runTask() throws Exception {
        StaticPost newProgram = FenixFramework.getDomainObject("852031317230556");
        Post program = FenixFramework.getDomainObject("563499709331449");
        if (program == null) {
            taskLog("O post referente a %s não existe%n", newProgram.getName());
        } else {
            newProgram.setPost(program);
        }

        StaticPost newDuvidas = FenixFramework.getDomainObject("852031317230557");
        Post duvidas = FenixFramework.getDomainObject("563499709331451");
        if (program == null) {
            taskLog("O post referente a %s não existe%n", duvidas.getName());
        } else {
            newDuvidas.setPost(duvidas);
        }

        StaticPost newNaoPresencial = FenixFramework.getDomainObject("852031317230558");
        Post presencial = FenixFramework.getDomainObject("1689399616165411");
        if (presencial == null) {
            taskLog("O post referente a %s não existe%n", newNaoPresencial.getName());
        } else {
            newNaoPresencial.setPost(presencial);
        }

        StaticPost newTeoricas = FenixFramework.getDomainObject("852031317230559");
        Post teoricas = FenixFramework.getDomainObject("563499709331435");
        if (teoricas == null) {
            taskLog("O post referente a %s não existe%n", newTeoricas.getName());
        } else {
            newTeoricas.setPost(teoricas);
        }

        StaticPost newPraticas = FenixFramework.getDomainObject("852031317230560");
        Post praticas = FenixFramework.getDomainObject("563499709331434");
        if (praticas == null) {
            taskLog("O post referente a %s não existe%n", newPraticas.getName());
        } else {
            newPraticas.setPost(praticas);
        }

        StaticPost newEnunciados = FenixFramework.getDomainObject("852031317230562");
        Post enunciados = FenixFramework.getDomainObject("563499709331440");
        if (enunciados == null) {
            taskLog("O post referente a %s não existe%n", newEnunciados.getName());
        } else {
            newEnunciados.setPost(enunciados);
        }

        StaticPost newFicha = FenixFramework.getDomainObject("852031317230563");
        Post ficha = FenixFramework.getDomainObject("563499709331443");
        if (ficha == null) {
            taskLog("O post referente a %s não existe%n", newFicha.getName());
        } else {
            newFicha.setPost(ficha);
        }

        StaticPost newLimites = FenixFramework.getDomainObject("852031317230564");
        Post limites = FenixFramework.getDomainObject("563499709331495");
        if (limites == null) {
            taskLog("O post referente a %s não existe%n", newLimites.getName());
        } else {
            newLimites.setPost(limites);
        }

        StaticPost newDocsSuporte = FenixFramework.getDomainObject("852031317230565");
        Post docsSuporte = FenixFramework.getDomainObject("563499709331489");
        if (docsSuporte == null) {
            taskLog("O post referente a %s não existe%n", newDocsSuporte.getName());
        } else {
            newDocsSuporte.setPost(docsSuporte);
        }

        StaticPost newDocsApoio = FenixFramework.getDomainObject("852031317230566");
        Post docsApoio = FenixFramework.getDomainObject("563499709331438");
        if (docsApoio == null) {
            taskLog("O post referente a %s não existe%n", newDocsApoio.getName());
        } else {
            newDocsApoio.setPost(docsApoio);
        }

        StaticPost newCaracterizacao = FenixFramework.getDomainObject("852031317230567");
        Post caracterizacao = FenixFramework.getDomainObject("563499709331447");
        if (caracterizacao == null) {
            taskLog("O post referente a %s não existe%n", newCaracterizacao.getName());
        } else {
            newCaracterizacao.setPost(caracterizacao);
        }

        StaticPost newSwot = FenixFramework.getDomainObject("852031317230568");
        Post swot = FenixFramework.getDomainObject("563499709331445");
        if (swot == null) {
            taskLog("O post referente a %s não existe%n", newSwot.getName());
        } else {
            newSwot.setPost(swot);
        }

        StaticPost newExames1718 = FenixFramework.getDomainObject("852031317230570");
        Post exames1718 = FenixFramework.getDomainObject("563499709331431");
        if (exames1718 == null) {
            taskLog("O post referente a %s não existe%n", newExames1718.getName());
        } else {
            newExames1718.setPost(exames1718);
        }

        StaticPost newExameTipo = FenixFramework.getDomainObject("852031317230571");
        Post exameTipo = FenixFramework.getDomainObject("563499709331432");
        if (exameTipo == null) {
            taskLog("O post referente a %s não existe%n", newExameTipo.getName());
        } else {
            newExameTipo.setPost(exameTipo);
        }

        StaticPost newAvaliacao = FenixFramework.getDomainObject("852031317230572");
        Post avaliacao = FenixFramework.getDomainObject("563499709331436");
        if (avaliacao == null) {
            taskLog("O post referente a %s não existe%n", newAvaliacao.getName());
        } else {
            newAvaliacao.setPost(avaliacao);
        }
    }
}
