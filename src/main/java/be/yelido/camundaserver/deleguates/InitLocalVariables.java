package be.yelido.camundaserver.deleguates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class InitLocalVariables implements JavaDelegate {
    private Expression var1;
    private Expression var2;
    private Expression var3;
    private Expression var4;
    private Expression var5;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String varName;

        if(var1 != null) {
            varName = (String) var1.getValue(execution);
            execution.setVariableLocal(varName, "");
        }

        if(var2 != null) {
            varName = (String) var2.getValue(execution);
            execution.setVariableLocal(varName, "");
        }

        if(var3 != null) {
            varName = (String) var3.getValue(execution);
            execution.setVariableLocal(varName, "");
        }

        if(var4 != null) {
            varName = (String) var3.getValue(execution);
            execution.setVariableLocal(varName, "");
        }

        if(var5 != null) {
            varName = (String) var3.getValue(execution);
            execution.setVariableLocal(varName, "");
        }

    }
}
