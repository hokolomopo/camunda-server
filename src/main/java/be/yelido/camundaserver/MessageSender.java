package be.yelido.camundaserver;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("messageSender")
public class MessageSender {

    public void sendMsgToParentProcess(String msg, DelegateExecution execution) throws Exception {
        DelegateExecution currentExec = execution.getProcessInstance();
        String currentId = execution.getProcessInstance().getId();

        DelegateExecution superExec = execution.getProcessInstance().getSuperExecution();
        String superId = execution.getProcessInstance().getSuperExecution().getProcessInstanceId();

        System.out.println("Send message to " + superId);

        execution.getProcessEngine().getRuntimeService().createMessageCorrelation(msg)
                .processInstanceId(execution.getProcessInstance().getSuperExecution().getProcessInstanceId()).correlate();
    }
}
