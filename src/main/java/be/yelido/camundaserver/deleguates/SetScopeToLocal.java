package be.yelido.camundaserver.deleguates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class SetScopeToLocal implements JavaDelegate {
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
            System.out.println("Set variable scope to local : " + varName);
            setVarToLocal(varName, execution);
        }
        if(var2 != null) {
            varName = (String) var2.getValue(execution);
            setVarToLocal(varName, execution);
        }
        if(var3 != null) {
            varName = (String) var3.getValue(execution);
            setVarToLocal(varName, execution);
        }
        if(var4 != null) {
            varName = (String) var4.getValue(execution);
            setVarToLocal(varName, execution);
        }
        if(var5 != null) {
            varName = (String) var5.getValue(execution);
            setVarToLocal(varName, execution);
        }

    }

    private void setVarToLocal(String varName, DelegateExecution execution){
        Object varValue = execution.getVariable(varName);
        execution.removeVariable(varName);
        execution.setVariableLocal(varName, varValue);
    }
}
