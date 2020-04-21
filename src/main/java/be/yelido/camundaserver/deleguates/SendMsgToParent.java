package be.yelido.camundaserver.deleguates;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.runtime.MessageCorrelationBuilder;

public class SendMsgToParent implements JavaDelegate {
    private Expression msgName;
    private Expression var1;
    private Expression var2;
    private Expression var3;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String msgNameStr = (String) msgName.getValue(execution);

        RuntimeService runtimeService = execution.getProcessEngine().getRuntimeService();
        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(msgNameStr)
                .processInstanceId(execution.getProcessInstance().getSuperExecution().getProcessInstanceId());

        if (var1 != null)
            setVariable(var1, execution, builder);
        if (var2 != null)
            setVariable(var2, execution, builder);
        if (var3 != null)
            setVariable(var3, execution, builder);

        builder.correlate();
    }

    void setVariable(Expression var, DelegateExecution execution, MessageCorrelationBuilder builder){
        String varName = (String) var.getValue(execution);
        Object varObj = execution.getVariable(varName);
        builder.setVariable(varName, varObj);
    }
}
