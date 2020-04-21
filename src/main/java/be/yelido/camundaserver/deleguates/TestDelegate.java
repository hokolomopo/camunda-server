package be.yelido.camundaserver.deleguates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("testDelegate")
public class TestDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution delegateExecution) throws Exception {
        Object o = delegateExecution.getVariable("header_destination");
        delegateExecution.setVariable("header_destination", o);
    }
}
