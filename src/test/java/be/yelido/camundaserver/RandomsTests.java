package be.yelido.camundaserver;

import be.yelido.camunda.module.data.dto.ProcessInstance;
import be.yelido.camunda.module.data.dto.Variable;
import be.yelido.camunda.module.data.dto.VariableValueInfo;
import be.yelido.camunda.module.data.request.CamundaType;
import be.yelido.camunda.module.data.request.StartProcessInstanceParameters;
import be.yelido.camunda.module.rest.CamundaRestTemplate;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class RandomsTests {
    public static final String url = "http://localhost:8083/rest";


    @Test
    void testMultiInstanceProcess() throws IOException {
//        CamundaRestTemplate restTemplate = new CamundaRestTemplate(url);
//
//        // Create Process Instance
//        ArrayList<String> c = new ArrayList<>();
//        c.add("BK1");
//        c.add("BK2");
//        c.add("BK3");
//        HashMap<String, Variable> variables = new HashMap<>();
//        variables.put("loopCollection", VariableUtil.createVariableFromCollection(c));
//        StartProcessInstanceParameters params = new StartProcessInstanceParameters( "BK", variables);
//        ProcessInstance p = restTemplate.startProcessInstanceByKey("testMultiInstanceSubrocess", params);

    }

}
