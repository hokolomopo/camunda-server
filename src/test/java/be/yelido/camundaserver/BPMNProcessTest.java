package be.yelido.camundaserver;

import be.yelido.camunda.module.data.dto.Execution;
import be.yelido.camunda.module.data.dto.ProcessInstance;
import be.yelido.camunda.module.data.dto.Variable;
import be.yelido.camunda.module.data.dto.VariableValueInfo;
import be.yelido.camunda.module.data.request.*;
import be.yelido.camunda.module.rest.CamundaRestTemplate;
import be.yelido.camunda.module.util.CamundaMonitor;
import be.yelido.camunda.module.util.CamundaToken;
import be.yelido.camunda.module.util.VariableUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jdk.nashorn.internal.parser.Token;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BPMNProcessTest {
    public static final String url = "http://localhost:8083/rest";

    @Test
    void runUploadAuditableProcess() throws InterruptedException, JsonProcessingException {
        //<editor-fold desc="start process and first steps">
        CamundaMonitor camundaMonitor = new CamundaMonitor(url);
        MessageCorrelationParameters messageCorrelationParameters;
        String tmpBK;

        // Create Process Instance
        StartProcessInstanceParameters startProcessInstanceParameters = new StartProcessInstanceParameters(UUID.randomUUID().toString(), null);
        ProcessInstance p = camundaMonitor.getRestTemplate().startProcessInstanceByKey("Upload_Auditable", startProcessInstanceParameters);
        assertNotNull(p);
        CamundaToken token = new CamundaToken(startProcessInstanceParameters.getBusinessKey());

        // Send message
//        messageCorrelationParameters = new MessageCorrelationParameters("UploadServiceImpl_uploadAuditable_msg", null);
//        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        camundaMonitor.correlateMessage(token, "UploadServiceImpl_uploadAuditable_msg", null);

        /*---------------------------------------
        Send msg to MQ => start a new sub process
        ---------------------------------------*/
        Thread.sleep(100);

        // Send message
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        ;
        tmpBK = UUID.randomUUID().toString();
        HashMap<String, Variable> vars = VariableUtil.mapFromSingleVariable("BK_JIOUpload", new Variable(tmpBK, CamundaType.STRING));
        messageCorrelationParameters = new MessageCorrelationParameters("SynchroController_integrateDataSource_msg", vars);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        token = new CamundaToken(tmpBK);
        Thread.sleep(100);
        //</editor-fold>

        /*---------------------------------------
         JIO_UPLOAD sub-process
        ---------------------------------------*/
        CamundaToken[] tokens = runJioUploadSubProcess(token, camundaMonitor);
        CamundaToken tokenDasys = tokens[0];
        CamundaToken tokenLegacy = tokens[1];

        /*---------------------------------------
         ESB Notifs Dasys
        ---------------------------------------*/
        Thread.sleep(100);
        runEsbNotifSubProcess(tokenDasys, camundaMonitor, null, "DASYS");


        /*---------------------------------------
         ESB Notifs Legacy
        ---------------------------------------*/
        Thread.sleep(100);
        runEsbNotifSubProcess(tokenLegacy, camundaMonitor, null, "LEGACY");
    }

    private CamundaToken[] runJioUploadSubProcess(CamundaToken token, CamundaMonitor camundaMonitor) throws InterruptedException {
        MessageCorrelationParameters messageCorrelationParameters = new MessageCorrelationParameters("JioUploadIntegrationProcessor_process_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(100);

        /*---------------------------------------
         Integrate AuditableT0
        ---------------------------------------*/
        CamundaToken tokenMergeT0 = CamundaToken.randomBKToken(token);
        HashMap<String, Variable> variableMap = VariableUtil.mapFromSingleVariable("mergeAuditableT0_BK", new Variable(tokenMergeT0.getCurrentProcessBK()));
        messageCorrelationParameters = new MessageCorrelationParameters("AuditableT0_Integrate_msg", variableMap);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        Thread.sleep(50);
        runMergeAuditableSubProcess(tokenMergeT0, camundaMonitor, 2);

        Thread.sleep(50);

        messageCorrelationParameters = new MessageCorrelationParameters("AuditableT0_MergeDAO_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        Thread.sleep(50);

        messageCorrelationParameters = new MessageCorrelationParameters("AuditableT0_UpdatePostits_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);


        String tmpBK = UUID.randomUUID().toString();
        HashMap<String, Variable> variableMap2 = VariableUtil.mapFromSingleVariable("BK_Dasys", new Variable(tmpBK, CamundaType.STRING));
        messageCorrelationParameters = new MessageCorrelationParameters("AuditableT0_Integrate_msg", variableMap2);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        CamundaToken tokenDasys = new CamundaToken(tmpBK);

        Thread.sleep(50);

        tmpBK = UUID.randomUUID().toString();
        HashMap<String, Variable> variableMap1 = VariableUtil.mapFromSingleVariable("BK_Legacy", new Variable(tmpBK, CamundaType.STRING));
        messageCorrelationParameters = new MessageCorrelationParameters("IntegrationController_SendToLegacy_msg", variableMap1);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        CamundaToken tokenLegacy = new CamundaToken(tmpBK);

        CamundaToken[] arr = {tokenDasys, tokenLegacy};
        return arr;
    }

    private void runEsbNotifSubProcess(CamundaToken token, CamundaMonitor camundaMonitor, String destination, String processor) throws InterruptedException {
        HashMap<String, Variable> variableMap2 = new HashMap<String, Variable>();
        variableMap2.put("headerDestination", new Variable(destination, CamundaType.STRING));
        variableMap2.put("headerProcessor", new Variable(processor, CamundaType.STRING));
        MessageCorrelationParameters messageCorrelationParameters = new MessageCorrelationParameters("ESBNotificationProcessor_process_msg", variableMap2);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        Thread.sleep(100);


        HashMap<String, Variable> variableMap3 = new HashMap<>();
        ArrayList<String> BKs = new ArrayList<>(); BKs.add(UUID.randomUUID().toString()); BKs.add(UUID.randomUUID().toString());
        variableMap3.put("restCallsBKs", VariableUtil.createVariableFromCollection(BKs));
        messageCorrelationParameters = new MessageCorrelationParameters("ESBNotificationProcessor_end_msg", variableMap3);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

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

    private void runMergeAuditableSubProcess(CamundaToken token, CamundaMonitor camundaMonitor, int numberOfEncounters) throws InterruptedException {

        ArrayList<String> ids = new ArrayList<>();
        for(int i=0;i < numberOfEncounters;i++)
            ids.add(String.valueOf(i));
        HashMap<String, Variable> variableMap = VariableUtil.mapFromSingleVariable("encouters_ids", VariableUtil.createVariableFromCollection(ids));
        MessageCorrelationParameters messageCorrelationParameters = new MessageCorrelationParameters("Merge_Encounters_msg", variableMap);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        for(int i=0;i < numberOfEncounters;i++){
            Thread.sleep(100);

            HashMap<String, Variable> variableMap2 = VariableUtil.mapFromSingleVariable("encounterUpdated", new Variable(true, CamundaType.BOOLEAN));
            messageCorrelationParameters = new MessageCorrelationParameters("Check_Updates_"+ids.get(i)+"_msg", variableMap2);
            camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        }
        Thread.sleep(100);

        messageCorrelationParameters = new MessageCorrelationParameters("MergeAcBases_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        Thread.sleep(100);

        messageCorrelationParameters = new MessageCorrelationParameters("Merge_AcLinks", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
    }

    private void raiseError(CamundaToken token, CamundaMonitor camundaMonitor) throws InterruptedException {
        Map<String, Variable> vars = VariableUtil.mapFromSingleVariable("error_message", new Variable("Error msg", CamundaType.STRING));
        MessageCorrelationParameters messageCorrelationParameters = new MessageCorrelationParameters("Raise_Error_msg", vars);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        Thread.sleep(50);
        camundaMonitor.raiseIncident(token, "Incident_msg", "This is an incident");
    }

    @Test
    void runTestProcess() throws InterruptedException, JsonProcessingException {
        CamundaMonitor camundaMonitor = new CamundaMonitor(url);
        MessageCorrelationParameters messageCorrelationParameters;

        // Create Process Instance
        StartProcessInstanceParameters startProcessInstanceParameters = new StartProcessInstanceParameters(UUID.randomUUID().toString(), null);
        ProcessInstance p = camundaMonitor.getRestTemplate().startProcessInstanceByKey("test", startProcessInstanceParameters);
        assertNotNull(p);
        CamundaToken token = new CamundaToken(startProcessInstanceParameters.getBusinessKey());

        // Send message
        HashMap<String, Variable> map = new HashMap<String, Variable>();
        map.put("test", new Variable("Hey"));
        messageCorrelationParameters = new MessageCorrelationParameters("wait", map);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        messageCorrelationParameters = new MessageCorrelationParameters("wait", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        map.clear();
        map.put("test", new Variable("Hoi"));
        messageCorrelationParameters = new MessageCorrelationParameters("wait", map);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        messageCorrelationParameters = new MessageCorrelationParameters("wait", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

    }
}
