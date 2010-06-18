/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.activiti.impl.bpmn;

import java.io.ByteArrayInputStream;
import java.util.Map;
import java.util.logging.Logger;

import org.activiti.Configuration;
import org.activiti.impl.bpmn.parser.BpmnParse;
import org.activiti.impl.bpmn.parser.BpmnParser;
import org.activiti.impl.bytes.ByteArrayImpl;
import org.activiti.impl.definition.ProcessDefinitionDbImpl;
import org.activiti.impl.definition.ProcessDefinitionImpl;
import org.activiti.impl.persistence.PersistenceSession;
import org.activiti.impl.repository.Deployer;
import org.activiti.impl.repository.DeploymentImpl;
import org.activiti.impl.repository.ProcessCache;
import org.activiti.impl.time.Clock;
import org.activiti.impl.tx.TransactionContext;


/**
 * @author Tom Baeyens
 */
public class BpmnDeployer implements Deployer {
  
  private static final Logger LOG = Logger.getLogger(BpmnDeployer.class.getName());;
  
  public static final String BPMN_RESOURCE_SUFFIX = "bpmn20.xml";
  
  public void deploy(DeploymentImpl deployment, TransactionContext transactionContext) {
    
    saveDeployment(deployment, transactionContext);
    Map<String, ByteArrayImpl> resources = deployment.getResources();
    
    for (String resourceName: resources.keySet()) {
      
      LOG.info("Processing resource " + resourceName);
      if (resourceName.endsWith(BPMN_RESOURCE_SUFFIX)) {
        ByteArrayImpl resource = resources.get(resourceName);
        byte[] bytes = resource.getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        BpmnParse bpmnParse = BpmnParser.INSTANCE
          .createParse()
          .processDefinitionClass(ProcessDefinitionDbImpl.class)
          .sourceInputStream(inputStream)
          .execute();
        
        ProcessCache processCache = transactionContext.getProcessEngine()
          .getConfigurationObject(Configuration.NAME_PROCESSCACHE,ProcessCache.class);
        
        for (ProcessDefinitionImpl processDefinition: bpmnParse.getProcessDefinitions()) {
          processDefinition.setDeployment(deployment);
          processDefinition.setNew(deployment.isNew());
          processCache.setProcessDefinition(processDefinition);
        }

      }  
    }
    
  }

  protected void saveDeployment(DeploymentImpl deployment, TransactionContext transactionContext) {
    PersistenceSession persistenceSession = transactionContext.getTransactionalObject(PersistenceSession.class);
    if (deployment.isNew()) {
      deployment.setDeploymentTime(Clock.getCurrentTime());
      persistenceSession.insertDeployment(deployment);
    }
  }

  public void delete(DeploymentImpl deployment, TransactionContext transactionContext) {
  }
}
