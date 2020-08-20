package be.yelido.camundaserver;

import be.yelido.camunda.module.data.dto.ProcessInstance;
import be.yelido.camunda.module.data.dto.Variable;
import be.yelido.camunda.module.data.ids.*;
import be.yelido.camunda.module.data.request.*;
import be.yelido.camunda.module.rest.CamundaRestTemplate;
import be.yelido.camunda.module.util.CamundaMonitor;
import be.yelido.camunda.module.util.CamundaToken;
import be.yelido.camunda.module.util.VariableUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Ignore
public class BPMNProcessTest {
    public static final String url = "http://localhost:8083/rest";

    private ObjectMapper objectMapper= new ObjectMapper();

    private String userId;
    private AuditableT0Id auditableT0Id;
    private ArrayList<AuditableT1Id> auditableT1Ids = new ArrayList<>();

    @Test
    void runUploadAuditableProcess() throws InterruptedException, JsonProcessingException {
        //<editor-fold desc="start process and first steps">

        int numberOfAuditableT1 = 2;

        Random r = new Random();
        userId = String.valueOf(r.nextInt());
        auditableT0Id = new AuditableT0Id(UUID.randomUUID().toString(), userId, String.valueOf(r.nextInt()), String.valueOf((char)(r.nextInt(26) + 'a')));
        for(int i = 0;i < numberOfAuditableT1;i++)
            auditableT1Ids.add(new AuditableT1Id(UUID.randomUUID().toString(), String.valueOf(r.nextInt()), String.valueOf(r.nextInt()), String.valueOf((char)(r.nextInt(26) + 'a')), String.valueOf(r.nextInt())));


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
        Thread.sleep(300);

        // Send message
        tmpBK = UUID.randomUUID().toString();
        HashMap<String, Variable> vars = VariableUtil.mapFromSingleVariable("BK_JIOUpload", new Variable(tmpBK, CamundaType.STRING));
        messageCorrelationParameters = new MessageCorrelationParameters("SynchroController_integrateDataSource_msg", vars);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        token = new CamundaToken(tmpBK);
        Thread.sleep(300);
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
        Thread.sleep(300);
        runEsbNotifSubProcess(tokenDasys, camundaMonitor, null, "DASYS");


        /*---------------------------------------
         ESB Notifs Legacy
        ---------------------------------------*/
        Thread.sleep(300);
        runEsbNotifSubProcess(tokenLegacy, camundaMonitor, null, "LEGACY");
    }

    private CamundaToken[] runJioUploadSubProcess(CamundaToken token, CamundaMonitor camundaMonitor) throws InterruptedException {
        MessageCorrelationParameters messageCorrelationParameters = new MessageCorrelationParameters("JioUploadIntegrationProcessor_process_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);

        /*---------------------------------------
         Integrate AuditableT0
        ---------------------------------------*/
        CamundaToken tokenMergeT0 = CamundaToken.randomBKToken(token);
        HashMap<String, Variable> variableMap = VariableUtil.mapFromSingleVariable("mergeAuditableT0_BK", new Variable(tokenMergeT0.getCurrentProcessBK()));
        variableMap.putAll(auditableT0Id.toVariableMap(objectMapper.convertValue(auditableT0Id, Map.class)));
        messageCorrelationParameters = new MessageCorrelationParameters("AuditableT0_Merge_msg", variableMap);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        Thread.sleep(300);
        runMergeAuditableSubProcess(tokenMergeT0, camundaMonitor, 1);

        Thread.sleep(300);
        messageCorrelationParameters = new MessageCorrelationParameters("AuditableT0_MergeDAO_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        Thread.sleep(300);

        messageCorrelationParameters = new MessageCorrelationParameters("AuditableT0_UpdatePostits_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        Thread.sleep(300);

        int numberOfAuditableT1 = auditableT1Ids.size();
        variableMap = VariableUtil.mapFromSingleVariable("number_of_AuditableT1", new Variable(numberOfAuditableT1, CamundaType.INT));
        messageCorrelationParameters = new MessageCorrelationParameters("AuditableT1_Integrate_msg", variableMap);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);


        for(int i =0; i < numberOfAuditableT1;i++){
            Thread.sleep(300);
            CamundaToken mergeT1Token = CamundaToken.randomBKToken(token);
            variableMap = VariableUtil.mapFromSingleVariable("mergeAuditableT1_BK", new Variable(mergeT1Token.getCurrentProcessBK()));
            variableMap.putAll(auditableT1Ids.get(i).toVariableMap(objectMapper.convertValue(auditableT1Ids.get(i), Map.class)));
            messageCorrelationParameters = new MessageCorrelationParameters("AuditableT1_Merge_msg", variableMap);
            camundaMonitor.correlateMessage(token, messageCorrelationParameters);

            //TODO delete this
//            Thread.sleep(300);
//            if(i>0)
//                raiseError(token, camundaMonitor, false);

            Thread.sleep(300);
            runMergeAuditableSubProcess(mergeT1Token, camundaMonitor, 1);

            Thread.sleep(300);
            messageCorrelationParameters = new MessageCorrelationParameters("AuditableT1_MergeDAO_msg", null);
            camundaMonitor.correlateMessage(token, messageCorrelationParameters);

            Thread.sleep(300);
            messageCorrelationParameters = new MessageCorrelationParameters("AuditableT1_UpdatePostits_msg", null);
            camundaMonitor.correlateMessage(token, messageCorrelationParameters);

            Thread.sleep(300);
            messageCorrelationParameters = new MessageCorrelationParameters("AuditableT1_UpdateReports_msg", null);
            camundaMonitor.correlateMessage(token, messageCorrelationParameters);

            Thread.sleep(300);
            messageCorrelationParameters = new MessageCorrelationParameters("AuditableT1_ReintegrationController_msg", null);
            camundaMonitor.correlateMessage(token, messageCorrelationParameters);

            Thread.sleep(300);
            messageCorrelationParameters = new MessageCorrelationParameters("AuditableT1_CheckSynchroAssignment_msg", null);
            camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        }

        String tmpBK = UUID.randomUUID().toString();
        HashMap<String, Variable> variableMap2 = VariableUtil.mapFromSingleVariable("BK_Dasys", new Variable(tmpBK, CamundaType.STRING));
        messageCorrelationParameters = new MessageCorrelationParameters("IntegrationController_SendToDasys_msg", variableMap2);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        CamundaToken tokenDasys = new CamundaToken(tmpBK);

        Thread.sleep(300);

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

        Thread.sleep(300);


        HashMap<String, Variable> variableMap3 = new HashMap<>();
        ArrayList<CamundaToken> tokens = new ArrayList<>();
        for(AuditableT1Id id : auditableT1Ids)
            tokens.add(CamundaToken.randomBKToken(token));
        ArrayList<String> s = new ArrayList<>();
        for(CamundaToken t : tokens)
            s.add(t.getCurrentProcessBK());
        variableMap3.put("RestCalls_BKs", VariableUtil.createVariableFromCollection(s));
        messageCorrelationParameters = new MessageCorrelationParameters("ESBNotificationProcessor_end_msg", variableMap3);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        Thread.sleep(300);

        for (int i = 0;i < auditableT1Ids.size();i++){
            CamundaToken t = tokens.get(i);
            // Find newly created sub process
            ProcessInstanceQuery restInstanceQuery = ProcessInstanceQuery.createQuery()
                    .businessKey(t.getCurrentProcessBK())
                    .build();

            if(processor.equals("LEGACY"))
                runProcessWorkerSubProcess(t, camundaMonitor, new ToProcessLegacyDTOId(auditableT1Ids.get(i).getRrnr(), userId));

            else if(processor.equals("DASYS"))
                runProcessEmployeeSubProcess(t, camundaMonitor);

        }

    }

    private void runMergeAuditableSubProcess(CamundaToken token, CamundaMonitor camundaMonitor, int numberOfEncounters) throws InterruptedException {
        Thread.sleep(300);

        HashMap<String, Variable> variableMap = VariableUtil.mapFromSingleVariable("number_of_encounters", new Variable(numberOfEncounters, CamundaType.INT));
        MessageCorrelationParameters messageCorrelationParameters = new MessageCorrelationParameters("Merge_Encounters_msg", variableMap);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);

        for(int i=0;i < numberOfEncounters;i++){
            Thread.sleep(300);

            CamundaToken t = CamundaToken.randomBKToken(token);
            HashMap<String, Variable> variableMap2 = VariableUtil.mapFromSingleVariable("encounterUpdated", new Variable(true, CamundaType.BOOLEAN));
            variableMap2.put("BK_UpdatePrestation", new Variable(t.getCurrentProcessBK()));
            messageCorrelationParameters = new MessageCorrelationParameters("Check_Updates_msg", variableMap2);
            camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        }
        Thread.sleep(300);

        messageCorrelationParameters = new MessageCorrelationParameters("MergeAcBases_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        Thread.sleep(300);

        messageCorrelationParameters = new MessageCorrelationParameters("Merge_AcLinks", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
    }

    private void runProcessWorkerSubProcess(CamundaToken token, CamundaMonitor camundaMonitor, ToProcessLegacyDTOId id) throws InterruptedException {

        HashMap<String, Variable> variableMap = new HashMap<>();
        variableMap.put("hasLabRequests", new Variable(true));
        variableMap.put("hasVaccinations", new Variable(true));
        variableMap.putAll(id.toVariableMap(objectMapper.convertValue(id, Map.class)));
        MessageCorrelationParameters messageCorrelationParameters = new MessageCorrelationParameters("Check_Needed_Processing_msg", variableMap);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);

        CamundaToken legacyProcessorToken = CamundaToken.randomBKToken(token);
        variableMap = VariableUtil.mapFromSingleVariable("BK_LegacyProcessor", new Variable(legacyProcessorToken.getCurrentProcessBK()));
        messageCorrelationParameters = new MessageCorrelationParameters("Send_to_Legacy_Processor_msg", variableMap);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);

        runLegacyProcessorSubProcess(legacyProcessorToken, camundaMonitor, id);

        CamundaToken egelProcessorToken = CamundaToken.randomBKToken(token);
        variableMap = VariableUtil.mapFromSingleVariable("BK_EgelProcessor", new Variable(egelProcessorToken.getCurrentProcessBK()));
        messageCorrelationParameters = new MessageCorrelationParameters("Send_to_Egel_Processor_msg", variableMap);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);

        runEgelProcessorSubProcess(egelProcessorToken, camundaMonitor, new ToProcessEgeld(id.getRrnr(), id.getUserId()));

        int numberOfVaccinnes = 2;
        ArrayList<String> bkList = new ArrayList<>();
        ArrayList<CamundaToken> tokensList = new ArrayList<>();
        for(int i =0;i < numberOfVaccinnes;i++){
            CamundaToken token1 = CamundaToken.randomBKToken(token);
            tokensList.add(token1);
            bkList.add(token1.getCurrentProcessBK());
        }

        variableMap = VariableUtil.mapFromSingleVariable("Vaccinnet_BKs", VariableUtil.createVariableFromCollection(bkList));
        messageCorrelationParameters = new MessageCorrelationParameters("Send_to_Vaccinnet_Processor_msg", variableMap);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Random r = new Random();

        for(int i =0;i < numberOfVaccinnes;i++){
            Thread.sleep(300);
            CamundaToken tokenVaccine = tokensList.get(i);

            runVaccinnetProcessorSubProcess(tokenVaccine, camundaMonitor, new VaccinationDTOId(id.getRrnr(), String.valueOf(r.nextInt()), String.valueOf(r.nextInt())));
        }

    }

    private void runLegacyProcessorSubProcess(CamundaToken token, CamundaMonitor camundaMonitor, ToProcessLegacyDTOId id) throws InterruptedException {

        HashMap<String, Variable> variableMap = new HashMap<>();
        variableMap.putAll(id.toVariableMap(objectMapper.convertValue(id, Map.class)));
        MessageCorrelationParameters messageCorrelationParameters = new MessageCorrelationParameters("Start_Legacy_Processing_msg", variableMap);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);

        messageCorrelationParameters = new MessageCorrelationParameters("Administrative_Processor_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);

        messageCorrelationParameters = new MessageCorrelationParameters("Hazard_Processor_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);

        messageCorrelationParameters = new MessageCorrelationParameters("Hepatitis_Processor_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);

        messageCorrelationParameters = new MessageCorrelationParameters("Inability_Processor_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);

        messageCorrelationParameters = new MessageCorrelationParameters("Inactivity_Processor_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);

        messageCorrelationParameters = new MessageCorrelationParameters("Prestation_Processor_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);

        messageCorrelationParameters = new MessageCorrelationParameters("Service_Processor_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);

        messageCorrelationParameters = new MessageCorrelationParameters("Reintegration_Processor_msg", null);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);
    }

    private void runEgelProcessorSubProcess(CamundaToken token, CamundaMonitor camundaMonitor, ToProcessEgeld id) throws InterruptedException {

        HashMap<String, Variable> variableMap = new HashMap<>();
        variableMap.putAll(id.toVariableMap(objectMapper.convertValue(id, Map.class)));
        MessageCorrelationParameters messageCorrelationParameters = new MessageCorrelationParameters("EgelProcessor_msg", variableMap);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);

        int numberOfRequests = 5;

        variableMap = VariableUtil.mapFromSingleVariable("numberOfLabRequest", new Variable(numberOfRequests, CamundaType.INT));
        messageCorrelationParameters = new MessageCorrelationParameters("EgelController_msg", variableMap);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);


        ArrayList<String> egelUuids = new ArrayList<>();
        for(int i =0;i < numberOfRequests;i++){
            egelUuids.add(UUID.randomUUID().toString());
            variableMap = VariableUtil.mapFromSingleVariable("oldLabRequestUuid ", new Variable(String.valueOf(i), CamundaType.STRING));
            //variableMap.put("labRequestUUID", new Variable(egelUuids.get(i)));
            LabRequestDTOId labRequestDTOId = new LabRequestDTOId(egelUuids.get(i), String.valueOf(i), String.valueOf(i) , String.valueOf(i));
            variableMap.putAll(labRequestDTOId.toVariableMap(objectMapper.convertValue(labRequestDTOId, Map.class)));
            messageCorrelationParameters = new MessageCorrelationParameters("Create_new_lab_request_msg", variableMap);
            camundaMonitor.correlateMessage(token, messageCorrelationParameters);
            Thread.sleep(300);

            variableMap = VariableUtil.mapFromSingleVariable("labRequestStatus", new Variable("SENT"));
            messageCorrelationParameters = new MessageCorrelationParameters("Process_Egel_" + labRequestDTOId.getLabRequestUuid() + "_msg", variableMap);
            camundaMonitor.correlateMessage(null, messageCorrelationParameters);

            Thread.sleep(300);
        }
    }

    private void runVaccinnetProcessorSubProcess(CamundaToken token, CamundaMonitor camundaMonitor, VaccinationDTOId id) throws InterruptedException {

        Map<String, Variable> variableMap = id.toVariableMap(objectMapper.convertValue(id, Map.class));
        MessageCorrelationParameters messageCorrelationParameters = new MessageCorrelationParameters("Vaccinnet_Controller_msg", variableMap);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
    }

    private void runProcessEmployeeSubProcess(CamundaToken token, CamundaMonitor camundaMonitor) throws InterruptedException {

        HashMap<String, Variable> variableMap = new HashMap<>();
        CamundaToken dasysProcessorToken = CamundaToken.randomBKToken(token);
        variableMap = VariableUtil.mapFromSingleVariable("BK_DasysProcessor", new Variable(dasysProcessorToken.getCurrentProcessBK()));
        MessageCorrelationParameters messageCorrelationParameters = new MessageCorrelationParameters("Process_Employee_Start_msg", variableMap);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        Thread.sleep(300);
    }

    private void raiseIncident(CamundaToken token, CamundaMonitor camundaMonitor, boolean skipIncident) throws InterruptedException {
        Map<String, Variable> vars = VariableUtil.mapFromSingleVariable("error_message", new Variable("Error msg", CamundaType.STRING));
        MessageCorrelationParameters messageCorrelationParameters = new MessageCorrelationParameters("Raise_Error_msg", vars);
        camundaMonitor.correlateMessage(token, messageCorrelationParameters);

        if(!skipIncident) {
            Thread.sleep(300);
            camundaMonitor.raiseIncident(token, "Incident_msg", "This is an incident");
        }
        else {
            messageCorrelationParameters = new MessageCorrelationParameters("Incident_msg", null);
            camundaMonitor.correlateMessage(token, messageCorrelationParameters);
        }
    }

    @Test
    void runTestProcess() throws InterruptedException, JsonProcessingException {
        CamundaMonitor camundaMonitor = new CamundaMonitor(url);
        CamundaRestTemplate restTemplate = camundaMonitor.getRestTemplate();

        CamundaToken processWorkerToken = CamundaToken.randomBKToken();
        ArrayList<String> vaccinnetBks = new ArrayList<>();
        for(int i =0;i < 2;i++){
            CamundaToken token = CamundaToken.randomBKToken(processWorkerToken);
            vaccinnetBks.add(token.getCurrentProcessBK());
        }

        CamundaInstruction camundaInstruction = new CamundaInstruction(CamundaInstruction.START_AFTER_ACTIVITY, "Send_to_Vaccinnet_Processor_msg", null);
        ArrayList<CamundaInstruction> camundaInstructions = new ArrayList<>();
        camundaInstructions.add(camundaInstruction);
        StartProcessInstanceParameters p = new StartProcessInstanceParameters(processWorkerToken.getCurrentProcessBK(),
                VariableUtil.mapFromSingleVariable("Vaccinnet_BKs", VariableUtil.createVariableFromCollection(vaccinnetBks)),
                camundaInstructions);
        restTemplate.startProcessInstanceByKey("Process_Worker", p);

    }
}
