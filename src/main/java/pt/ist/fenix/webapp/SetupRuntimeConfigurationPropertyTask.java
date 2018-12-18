package pt.ist.fenix.webapp;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.fenixedu.bennu.GiafInvoiceConfiguration;
import org.fenixedu.bennu.scheduler.custom.CustomTask;
import org.fenixedu.commons.configuration.ConfigurationInvocationHandler;

import pt.ist.fenixframework.Atomic.TxMode;

public class SetupRuntimeConfigurationPropertyTask extends CustomTask {

    @Override
    public TxMode getTxMode() {
        return TxMode.READ;
    }

    @Override
    public void runTask() throws Exception {
        taskLog("Antes %s%n", GiafInvoiceConfiguration.getConfiguration().sapSyncActive());
        Field field = ConfigurationInvocationHandler.class.getDeclaredField("configs");
        field.setAccessible(true);

        Map<Class<?>, Object> configs = (Map<Class<?>, Object>) field.get(null);

        ConfigurationInvocationHandler handler = (ConfigurationInvocationHandler) Proxy
                .getInvocationHandler(configs.get(org.fenixedu.bennu.GiafInvoiceConfiguration.ConfigurationProperties.class));

        Field cacheField = ConfigurationInvocationHandler.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        Map<String, Object> cache = (Map<String, Object>) cacheField.get(handler);
        //cache.clear();
        cache.put("sapSyncActive", false);

        boolean sapSyncActive = GiafInvoiceConfiguration.getConfiguration().sapSyncActive();
        taskLog("%s%n", sapSyncActive);

        taskLog(GiafInvoiceConfiguration.getConfiguration().giafInvoiceDir());
    }

}
