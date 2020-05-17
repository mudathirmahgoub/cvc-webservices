/* This file is part of the cvc-webservices.
 *  Copyright (c) 2018 by the Board of Trustees of the University of Iowa
 *  Licensed under the Apache License, Version 2.0 (the "License"); you
 *  may not use this file except in compliance with the License.  You
 *  may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package cvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import cvc.Contracts.RawResults;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

@ServerEndpoint("/getResults/{id}")
public class ResultsEndpoint
{
    @OnOpen
    public void onOpen(@PathParam("id") String id, Session session, EndpointConfig config)
    {
        try
        {
            RawResults results = CvcClient.getRawResults(id);
            if (results != null)
            {
                // wait until the job finishes
                while (CvcContext.runningTasks.containsKey(id))
                {
                    // sleep for one second
                    Thread.sleep(1000);
                }

                results = CvcClient.getRawResults(id);
                results.jobId = id;
                results.jobFinished = true;

                // convert the results into json
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(results);

                // send the results
                session.getBasicRemote().sendText(json);
                // close the connection
                session.close();
            }
            else
            {
                session.getBasicRemote().sendText("Job " + id + " was not found");
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(Session session, String message)
    {
    }

    @OnError
    public void onError(Session session, Throwable error)
    {
    }

    @OnClose
    public void onClose(Session session, CloseReason reason)
    {
        System.out.println("Session " + session.getId() + " has ended");
        System.out.println(reason.toString());
    }
}
