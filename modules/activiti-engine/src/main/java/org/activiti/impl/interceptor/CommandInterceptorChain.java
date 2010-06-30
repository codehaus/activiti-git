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
package org.activiti.impl.interceptor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.activiti.ActivitiException;


/**
 * @author Dave Syer
 */
public class CommandInterceptorChain implements CommandExecutor {

  private static Logger log = Logger.getLogger(CommandInterceptorChain.class.getName());
  private final List<CommandInterceptor> chain = new ArrayList<CommandInterceptor>();
  
  private final CommandContextFactory commandContextFactory;
  
  public CommandInterceptorChain(CommandContextFactory commandContextFactory) {
    this.commandContextFactory = commandContextFactory;
  }
  
  public CommandInterceptorChain appendInterceptor(CommandInterceptor interceptor) {
    chain.add(interceptor);
    return this;
  }
  
  public <T> T execute(Command<T> command) {
    log.fine("");
    log.fine("=== starting command " + command + " ===========================================");
    CommandContext commandContext = commandContextFactory.createCommandContext(command);
    try {
      return new InternalChain(chain, commandContext).execute(command);

    } catch (Throwable exception) {
      commandContext.exception(exception);

      if (exception instanceof RuntimeException) {
        throw (RuntimeException) exception;
      } else if (exception instanceof Error) {
        throw (Error) exception;
      }
      throw new ActivitiException(exception.getMessage(), exception);

    } finally {
      commandContext.close();
      log.fine("=== command " + command + " finished ===========================================");
      log.fine("");
    }
  }
  
  private static class InternalChain implements CommandExecutor {
    
    private final Iterator<CommandInterceptor> chain;
    private final CommandContext context;

    public InternalChain(List<CommandInterceptor> chain, CommandContext context) {
      this.context = context;
      this.chain = chain.iterator();
    }

    public <T> T execute(Command<T> command) {
      if (chain.hasNext()) {
        return chain.next().invoke(this, context);
      }
      return  command.execute(context);
    }
    
  }

}
