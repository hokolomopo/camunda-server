package be.yelido.camundaserver.deleguates;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class InvalidateLabRequest implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        try {
            String oldLabRequestUuid = (String) execution.getVariable("oldLabRequestUUID");

            log.info("Invalidate old lab request [{}]", oldLabRequestUuid);

            RuntimeService runtimeService = execution.getProcessEngine().getRuntimeService();

            String signalName = "Cancel_" + oldLabRequestUuid + "_msg";

            Map<String, Object> vars = new HashMap<>();
            vars.put("labRequestStatus", "INVALIDATED");

            runtimeService
                    .createSignalEvent(signalName)
                    .setVariables(vars)
                    .send();
        }catch (Exception ignored){

        }

        return;
    }
}
