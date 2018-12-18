package pt.ist.fenix.webapp;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.academic.domain.Person;
import org.fenixedu.academic.domain.contacts.MobilePhone;
import org.fenixedu.academic.domain.contacts.Phone;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.scheduler.custom.CustomTask;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic.TxMode;

public class GetInfoForDelegatesInfoTask extends CustomTask {
 
    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }
 
    @Override
    public void runTask() throws Exception {
        String[] usernames = new String[] { "ist1100515","ist196694","ist196696","ist196700","ist193557","ist193557","ist196921","ist196942","ist199143","ist199160","ist199164","ist193796","ist196783","ist199013","ist199025","ist190578","ist193625","ist193644","ist193658","ist193644","ist196678","ist190546","ist191044","ist193535","ist196678","ist191044","ist189420","ist195530","ist195568","ist195591","ist195604","ist195631","ist195640","ist195652","ist198432","ist199211","ist192557","ist193881","ist195530","ist195568","ist189447","ist189520","ist190061","ist193881","ist193758","ist196837","ist196869","ist193687","ist193695","ist193712","ist193725","ist193728","ist193755","ist193758","ist196837","ist190751","ist193691","ist193695","ist196722","ist196751","ist197867","ist198932","ist198952","ist193602","ist196722","ist195744","ist195748","ist198915","ist199444","ist192634","ist192641","ist192627","ist195728","ist196690","ist199375","ist192596","ist195728","ist189592","ist424907","ist424907","ist186449","ist197251","ist196981","ist198500","ist190560","ist192718","ist193302","ist195778","ist195783","ist195826","ist195838","ist196375","ist198460","ist198718","ist199483","ist199514","ist199519","ist199544","ist150368","ist192653","ist192703","ist192718","ist193302","ist189683","ist190921","ist192718","ist186617","ist186638","ist186641","ist186641","ist196657","ist193498","ist187448","ist190529","ist190573","ist425845","ist425845","ist195884","ist199587","ist199613","ist199622","ist199623","ist192784","ist195884","ist189784","ist192784","ist186750","ist189784","ist186750","ist1100613","ist195923","ist195943","ist199642","ist199658","ist192806","ist192814","ist192832","ist192837","ist195923","ist195943","ist189809","ist189810","ist189825","ist186805","ist189809","ist1100608","ist195988","ist196078","ist197359","ist199786","ist190310","ist192886","ist195988","ist196008","ist196078","ist189956","ist190310","ist192886","ist186898","ist190310","ist186898","ist187005","ist177973","ist177085","ist177973","ist1100003","ist1100017","ist1100029","ist193128","ist196151","ist196240","ist196251","ist196271","ist196281","ist196309","ist196338","ist199952","ist190009","ist190084","ist190514","ist193081","ist193097","ist193098","ist193128","ist193179","ist193195","ist193197","ist196281","ist190009","ist190042","ist190101","ist190514","ist193098","ist193128","ist186932","ist187028","ist190009","ist425286","ist425399","ist425399","ist1100291","ist1100306","ist1100340","ist196566","ist193389","ist193413","ist193977","ist196566","ist190409","ist193413","ist425109","ist425109","ist188180","ist187620","ist190651","ist425929","ist170376","ist198888","ist187512","ist186389","ist186499","ist189409","ist189440","ist189447","ist189450","ist189471","ist189473","ist189514","ist189520","ist189535","ist190700","ist186389","ist187664","ist190714","ist190719","ist190751","ist190776","ist187664","ist196110","ist199853","ist199877","ist192984","ist192989","ist196110","ist186354","ist1100113","ist1100128","ist1100139","ist1100253","ist1100256","ist1100264","ist1100266","ist196349","ist196366","ist196387","ist196458","ist196484","ist196496","ist193220","ist193248","ist193250","ist193356","ist196366","ist190260","ist190340","ist190365","ist193356","ist187218","ist190260","ist198553","ist1100374","ist1100381","ist1100456","ist196574","ist196581","ist196616","ist193462","ist193478","ist193484","ist193492","ist196581","ist190462","ist190475","ist193484","ist187407","ist190475","ist197233","ist198545","ist198872","ist186592","ist424953","ist198525","ist198586" };
 
        StringBuilder builder = new StringBuilder();
        Stream.of(usernames).map(User::findByUsername).map(User::getPerson).forEach(p -> {
            builder.append(String.format("%s;%s;%s%n", p.getUsername(), p.getEmailForSendingEmails(), getMobileNumber(p)));
        });
 
        output("delegate_contacts.csv", builder.toString().getBytes(StandardCharsets.UTF_8));
    }
 
    private String getMobileNumber(Person p) {
        if (!Strings.isNullOrEmpty(p.getDefaultMobilePhoneNumber())) {
            return p.getDefaultMobilePhoneNumber();
        }
        String phones = p.getMobilePhones().stream().map(MobilePhone::getNumber).collect(Collectors.joining(" "));
        if (!Strings.isNullOrEmpty(phones)) {
            return phones;
        }
 
        phones = p.getPhones().stream().map(Phone::getNumber).collect(Collectors.joining(" "));
 
        if (!Strings.isNullOrEmpty(phones)) {
            return phones;
        }
 
        return "Sem numero";
    }
 
}