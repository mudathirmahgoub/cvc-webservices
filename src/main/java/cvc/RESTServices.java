/* This file is part of the cvc2-webservices.
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

import cvc.Contracts.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;


@Path("")
public class RESTServices
{
    private static final Logger log = Logger.getLogger(RESTServices.class);

    public static final String[] specialAttrs = {"code", "cvc"}; // these are attributes that cvc expects specifically

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response homePage()
    {
        InputStream inputStream = RESTServices.class.getResourceAsStream("/index.html");

        try
        {
            String html = IOUtils.toString(inputStream, Charset.defaultCharset());
            return Response.ok().entity(html).build();
        }
        catch (IOException e)
        {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/test")
    @Produces(MediaType.TEXT_PLAIN)
    public Response test()
    {
        return Response.ok().entity("Test service is working!").build();
    }

    @GET
    @Path("/examples")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getExamples()
    {
        try
        {
            File examplesDir = new File(CvcContext.examplesDirectory);

            Examples examples = new Examples();

            examples.names = examplesDir.list();

            return Response.ok().entity(examples).build();
        }
        catch (Exception e)
        {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/examples/{name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getExample(@PathParam("name") String name)
    {
        try
        {
            File exampleDir = new File(new File(CvcContext.examplesDirectory), name);

            if (! exampleDir.exists())
            {
                return Response.status(Status.NOT_FOUND)
                        .entity("could not find example with name: " + name)
                        .build();
            }

            File codeFile = new File(exampleDir, name + ".smt2");

            VerificationInput input = new VerificationInput();

            input.code = FileUtils.readFileToString(codeFile, Charset.defaultCharset());

            return Response.ok().entity(input).build();
        }
        catch (Exception e)
        {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/arguments")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getCvcArguments()
    {
        return Response.ok().entity(CvcContext.cvcArguments).build();
    }

    @POST
    @Path("/saveJob")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response saveJob(Job job)
    {
        try
        {
            if(job.jobId == null)
            {
                job.jobId = Constants.tempPrefix + UUID.randomUUID().toString();
            }

            CvcClient.saveJob(job.jobId, job.code);

            JobInformation information = new JobInformation();
            information.jobId = job.jobId;
            return Response.ok().entity(information).build();
        }
        catch (Exception e)
        {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    /**
     * Submits code for starting a new CvcContext job. A job ID for referencing the job created by this request will be returns.
     *
     * @param verificationInput The request form sent by the client
     * @return The job ID of this job as a string
     */
    @POST
    @Path("/run")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response verify(VerificationInput verificationInput)
    {
        try
        {
            if(verificationInput.jobId == null)
            {
                // create new job Id
                verificationInput.jobId = Constants.tempPrefix + UUID.randomUUID().toString();
            }



            List<String> args = new ArrayList<>();
            for (Map.Entry<String, String> pair : verificationInput.arguments.entrySet())
            {
                String key = pair.getKey();
                if (Util.containsIgnoreCase(specialAttrs, key))
                {
                    continue; //we handle the special attrs differently
                }

                String value = pair.getValue();

                if(! Validator.isArgValid(key, value))
                {
                    return Response.status(Status.BAD_REQUEST)
                            .entity("Invalid argument: \"" + key + ": " + value + "\"")
                            .build();
                }

                Argument argument = CvcContext.cvcArguments.get(key);
                if (value == null || value.isEmpty())
                {
                    log.debug("found flag " + key);
                    args.add(argument.prefix + key);
                } else
                {
                    log.debug("found arg " + key + " = " + value);
                    //otherwise, add the name and value
                    args.add(argument.prefix + key);
                    args.add(value);
                }
            }

            Callable<Void> task = () -> CvcClient.createJob(verificationInput.jobId, verificationInput.code, args);

            // asynchronously submit the task
            Future<Void> future = CvcContext.getCvcExecutorService().submit(task);
            CvcContext.runningTasks.put(verificationInput.jobId, future);

            System.out.println("got this job ID = " + verificationInput.jobId);

            JobInformation information = new JobInformation();
            information.jobId = verificationInput.jobId;
            return Response.ok().entity((information)).build();
        }
        catch (Exception e)
        {
            log.error("run says " + e.getMessage(), e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage())
                    .build();
        }
    }

    @GET
    @Path("/getJob/{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getJob(@PathParam("id") String id)
    {
        try
        {
            File jobDir = new File(new File(CvcContext.jobsDirectory), id);

            if (! jobDir.exists())
            {
                return Response.status(Status.NOT_FOUND)
                        .entity("could not find job with id: " + id)
                        .build();
            }

            File codeFile = new File(jobDir, Constants.CODE_FILE);

            Job job = new Job();

            job.code = FileUtils.readFileToString(codeFile, Charset.defaultCharset());

            return Response.ok().entity(job).build();
        }
        catch (Exception e)
        {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/getRawResults/{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response getRawResults(@PathParam("id") String id)
    {
        try
        {
            RawResults results = CvcClient.getRawResults(id);
            results.jobId = id;
            results.jobFinished = ! CvcContext.runningTasks.containsKey(id);
            return Response.ok().entity(results).build();
        }
        catch (Exception e)
        {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    
    @GET
    @Path("/cancelJob/{id}")
    @Produces({MediaType.TEXT_PLAIN})
    public Response cancelJob(@PathParam("id") String id)
    {
        try
        {
            CvcClient.cancelJob(id);
            return Response.ok().build();
        }
        catch (Exception e)
        {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(e.getMessage()).build();
        }
    }
}
