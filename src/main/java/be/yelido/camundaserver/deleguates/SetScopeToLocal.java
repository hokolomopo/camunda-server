package be.yelido.camundaserver.deleguates;

import be.yelido.camundaserver.util.SimpleListStringParser;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class SetScopeToLocal implements JavaDelegate {
    private Expression vars;

    private SimpleListStringParser parser = new SimpleListStringParser();

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String listString = (String)vars.getValue(execution);

        for(String s : parser.parse(listString))
            setVarToLocal(s, execution);

    }

    private void setVarToLocal(String varName, DelegateExecution execution){

        Object varValue = execution.getVariable(varName);
        execution.removeVariable(varName);
        execution.setVariableLocal(varName, varValue);
    }
}
