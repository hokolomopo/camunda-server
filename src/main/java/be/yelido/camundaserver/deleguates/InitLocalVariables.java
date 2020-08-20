package be.yelido.camundaserver.deleguates;

import be.yelido.camundaserver.util.SimpleListStringParser;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class InitLocalVariables implements JavaDelegate {
    private Expression vars;
    private SimpleListStringParser parser = new SimpleListStringParser();

    @Override
    public void execute(DelegateExecution execution) throws Exception {

        String listString = (String)vars.getValue(execution);

        for(String varName : parser.parse(listString)){
            execution.setVariableLocal(varName, "");
        }
    }
}
