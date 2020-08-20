package be.yelido.camundaserver.deleguates;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.runtime.EventSubscription;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class InvalidateLabRequest implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        try {
            String oldLabRequestUuid = (String) execution.getVariable("oldLabRequestUuid");

            log.info("Invalidate old lab request [{}]", oldLabRequestUuid);

            RuntimeService runtimeService = execution.getProcessEngine().getRuntimeService();

            String messageName = "Cancel_" + oldLabRequestUuid + "_msg";

            Map<String, Object> vars = new HashMap<>();
            vars.put("labRequestStatus", "INVALIDATED");

            EventSubscription subscription = runtimeService.createEventSubscriptionQuery().eventName(messageName).singleResult();

            if(subscription != null)
                runtimeService.messageEventReceived(subscription.getEventName(), subscription.getExecutionId());

//            runtimeService
//                    .createMessageCorrelation(messageName)
//                    .setVariables(vars)
//                    .correlate();
        }catch (Exception ignored){
            System.err.println("Exception");
        }

        return;
    }
}
