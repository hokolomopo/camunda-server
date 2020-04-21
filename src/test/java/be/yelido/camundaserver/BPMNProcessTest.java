package be.yelido.camundaserver;

import be.yelido.camunda.module.data.dto.Execution;
import be.yelido.camunda.module.data.dto.ProcessInstance;
import be.yelido.camunda.module.data.dto.Variable;
import be.yelido.camunda.module.data.request.*;
import be.yelido.camunda.module.rest.CamundaRestTemplate;
import be.yelido.camunda.module.util.CamundaMonitor;
import be.yelido.camunda.module.util.CamundaToken;
import be.yelido.camunda.module.util.VariableUtil;
import jdk.nashorn.internal.parser.Token;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BPMNProcessTest {
    public static final String url = "http://localhost:8083/rest";

    @Test
    void runUploadAuditableProcess() throws InterruptedException {
        CamundaMonitor camundaMonitor = new CamundaMonitor(url);
        MessageCorrelationParameters messageCorrelationParameters;
        String tmpBK;

        // Create Process Instance
        StartProcessInstanceParameters startProcessInstanceParameters = new StartProcessInstanceParameters(UUID.randomUUID().toString(), null);
        ProcessInstance p = camundaMonitor.getRestTemplate().startProcessInstanceByKey("Upload_Auditable", startProcessInstanceParameters);
        assertNotNull(p);
        CamundaToken token = new CamundaToken(startProcessInstanceParameters.getBusinessKey());

        // Send message
        messageCorrelationParameters = new MessageCorrelationParameters("UploadServiceImpl_uploadAuditable_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        //-----------------------------------------
        //Send msg to MQ => start a new sub process
        //-----------------------------------------
        Thread.sleep(100);

        // Send message
        tmpBK = UUID.randomUUID().toString();
        HashMap<String, Variable> vars = VariableUtil.mapFromSingleVariable("BK_JIOUpload", new Variable(tmpBK, CamundaType.STRING));
        messageCorrelationParameters = new MessageCorrelationParameters("SynchroController_integrateDataSource_msg", vars);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        token = new CamundaToken(tmpBK);
        Thread.sleep(100);

        //-----------------------------------------
        // JIO_UPLOAD sub-process
        //-----------------------------------------
        messageCorrelationParameters = new MessageCorrelationParameters("JioUploadIntegrationProcessor_process_mdg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(100);

        messageCorrelationParameters = new MessageCorrelationParameters("IntegrationController_integrate_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        Thread.sleep(50);
        
        tmpBK = UUID.randomUUID().toString();
        HashMap<String, Variable> variableMap = VariableUtil.mapFromSingleVariable("BK_Dasys", new Variable(tmpBK, CamundaType.STRING));
        messageCorrelationParameters = new MessageCorrelationParameters("IntegrationController_SendToDasys_msg", variableMap);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        CamundaToken tokenDasys = new CamundaToken(tmpBK);

        tmpBK = UUID.randomUUID().toString();
        HashMap<String, Variable> variableMap1 = VariableUtil.mapFromSingleVariable("BK_Legacy", new Variable(tmpBK, CamundaType.STRING));
        messageCorrelationParameters = new MessageCorrelationParameters("IntegrationController_SendToLegacy_msg", variableMap1);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        CamundaToken tokenLegacy = new CamundaToken(tmpBK);

        //-----------------------------------------
        // ESB Notifs Legacy
        //-----------------------------------------
        Thread.sleep(100);

        HashMap<String, Variable> variableMap2 = new HashMap<String, Variable>();
        variableMap2.put("headerDestination", new Variable(null, CamundaType.STRING));
        variableMap2.put("headerProcessor", new Variable("LEGACY", CamundaType.STRING));
        messageCorrelationParameters = new MessageCorrelationParameters("ESBNotificationProcessor_process_msg", variableMap2);
        camundaMonitor.correlateMessage(tokenLegacy, messageCorrelationParameters);

        Thread.sleep(100);

        HashMap<String, Variable> variableMap3 = new HashMap<>();
        ArrayList<String> BKs = new ArrayList<>(); BKs.add(UUID.randomUUID().toString()); BKs.add(UUID.randomUUID().toString());
        variableMap3.put("restCallsBKs", VariableUtil.createVariableFromCollection(BKs));
        messageCorrelationParameters = new MessageCorrelationParameters("ESBNotificationProcessor_end_msg", variableMap3);
        camundaMonitor.correlateMessage(tokenLegacy, messageCorrelationParameters);

        Thread.sleep(100);

        for (String bk : BKs){
            // Find newly created sub process
            ProcessInstanceQuery restInstanceQuery = ProcessInstanceQuery.createQuery()
                    .businessKey(bk)
                    .processDefinitionKey("Orchestrator_Rest")
                    .build();
            ProcessInstance restCallInstance = camundaMonitor.getRestTemplate().searchProcessInstancesSingleResult(restInstanceQuery);
            assertNotNull(restCallInstance);
        }
    }
}
