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

    private Expression parentLevelExpr;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String msgNameStr = (String) msgName.getValue(execution);

        RuntimeService runtimeService = execution.getProcessEngine().getRuntimeService();

        String parentLevelString = "1000";
        if(parentLevelExpr != null)
            parentLevelString = (String) parentLevelExpr.getValue(execution);
        int parentLevel = Integer.parseInt(parentLevelString);

        DelegateExecution superExecution = execution;
        int i = 0;
        do {
            superExecution = superExecution.getProcessInstance().getSuperExecution();
            System.out.println(superExecution.getId());
            i ++;
        }while(superExecution.getProcessInstance().getSuperExecution() != null && i < parentLevel);

        MessageCorrelationBuilder builder = runtimeService.createMessageCorrelation(msgNameStr)
                .processInstanceId(superExecution.getProcessInstanceId());
        System.out.println("Sending msg type : " + msgNameStr);

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
