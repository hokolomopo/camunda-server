package be.yelido.camundaserver;

import be.yelido.camunda.module.data.dto.Execution;
import be.yelido.camunda.module.data.dto.ProcessInstance;
import be.yelido.camunda.module.data.dto.Variable;
import be.yelido.camunda.module.data.request.CamundaType;
import be.yelido.camunda.module.data.request.ExecutionQuery;
import be.yelido.camunda.module.data.request.MessageCorrelationParameters;
import be.yelido.camunda.module.data.request.ProcessInstanceQuery;
import be.yelido.camunda.module.rest.CamundaRestTemplate;
import jdk.nashorn.internal.parser.Token;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BPMNProcessTest {
    public static final String url = "http://localhost:8083/rest";

    @Test
    void runUploadAuditableProcess() throws InterruptedException {
        CamundaRestTemplate restTemplate = new CamundaRestTemplate(url);
        MessageCorrelationParameters messageCorrelationParameters;

        // Create Process Instance
        ProcessInstance p = restTemplate.startProcessInstanceByKey("Upload_Auditable", null);
        assertNotNull(p);
        CamundaToken token = new CamundaToken(p.getId(), p.getId());

        // Send message
        messageCorrelationParameters = new MessageCorrelationParameters("UploadServiceImpl_uploadAuditable_msg", token.getCurrentProcessId(), null);
        restTemplate.sendMessage(messageCorrelationParameters);

        //-----------------------------------------
        //Send msg to MQ => start a new sub process
        //-----------------------------------------
        Thread.sleep(100);

        // Send message
        messageCorrelationParameters = new MessageCorrelationParameters("SynchroController_integrateDataSource_msg", token.getCurrentProcessId(), null);
        restTemplate.sendMessage(messageCorrelationParameters);
        Thread.sleep(100);

        // Find newly created sub process
        ProcessInstanceQuery query = ProcessInstanceQuery.createQuery()
                .superProcessInstance(token.getCurrentProcessId())
                .processDefinitionKey("JIO_Upload")
                .build();
        ProcessInstance jioUpload = restTemplate.searchProcessInstancesSingleResult(query);
        token.setCurrentProcessId(jioUpload.getId());

        //-----------------------------------------
        // JIO_UPLOAD sub-process
        //-----------------------------------------
        messageCorrelationParameters = new MessageCorrelationParameters("JioUploadIntegrationProcessor_process_mdg", token.getCurrentProcessId(), null);
        restTemplate.sendMessage(messageCorrelationParameters);
        Thread.sleep(100);

        messageCorrelationParameters = new MessageCorrelationParameters("IntegrationController_integrate_msg", token.getCurrentProcessId(), null);
        restTemplate.sendMessage(messageCorrelationParameters);

        Thread.sleep(50);

//        messageCorrelationParameters = new MessageCorrelationParameters("msg", token.getCurrentProcessId(), null);
//        restTemplate.sendMessage(messageCorrelationParameters);
        System.out.println(token.getCurrentProcessId());

        HashMap<String, Variable> variableMap = new HashMap<String, Variable>();
        variableMap.put("BK_Dasys", new Variable("MyBK_Dasys", CamundaType.STRING));
        messageCorrelationParameters = new MessageCorrelationParameters("IntegrationController_SendToDasys_msg", token.getCurrentProcessId(), variableMap);
        restTemplate.sendMessage(messageCorrelationParameters);


        HashMap<String, Variable> variableMap1 = new HashMap<String, Variable>();
        variableMap1.put("BK_Legacy", new Variable("ThatsMyBK", CamundaType.STRING));
        messageCorrelationParameters = new MessageCorrelationParameters("IntegrationController_SendToLegacy_msg", token.getCurrentProcessId(), variableMap1);
        restTemplate.sendMessage(messageCorrelationParameters);

        Thread.sleep(100);

        ProcessInstanceQuery pInstanceQuery1 = ProcessInstanceQuery.createQuery().active(true).superProcessInstance(token.getSuperProcessId()).businessKey("ThatsMyBK").build();
        p = restTemplate.searchProcessInstancesSingleResult(pInstanceQuery1);
        System.out.println(token.getSuperProcessId());
        CamundaToken tokenLegacy = new CamundaToken(p.getId(), token.getSuperProcessId());

        //-----------------------------------------
        // ESB Notifs Legacy
        //-----------------------------------------
        Thread.sleep(100);

        HashMap<String, Variable> variableMap2 = new HashMap<String, Variable>();
        variableMap2.put("headerDestination", new Variable(null, CamundaType.STRING));
        variableMap2.put("headerProcessor", new Variable("LEGACY", CamundaType.STRING));
        messageCorrelationParameters = new MessageCorrelationParameters("ESBNotificationProcessor_process_msg", tokenLegacy.getCurrentProcessId(), variableMap2);
        restTemplate.sendMessage(messageCorrelationParameters);

        Thread.sleep(100);

        HashMap<String, Variable> variableMap3 = new HashMap<>();
        ArrayList<String> BKs = new ArrayList<>(); BKs.add("BK1"); BKs.add("BK2");
        variableMap3.put("restCallsBKs", VariableUtil.createVariableFromCollection(BKs));
        messageCorrelationParameters = new MessageCorrelationParameters("ESBNotificationProcessor_end_msg", tokenLegacy.getCurrentProcessId(), variableMap3);
        restTemplate.sendMessage(messageCorrelationParameters);
        System.out.println(tokenLegacy.getCurrentProcessId());

        Thread.sleep(100);

        for (String bk : BKs){
            // Find newly created sub process
            ProcessInstanceQuery restInstanceQuery = ProcessInstanceQuery.createQuery()
                    .superProcessInstance(tokenLegacy.getSuperProcessId())
                    .businessKey(bk)
                    .processDefinitionKey("Orchestrator_Rest")
                    .build();
            ProcessInstance restCallInstance = restTemplate.searchProcessInstancesSingleResult(restInstanceQuery);
            assertNotNull(restCallInstance);
        }
    }
}
