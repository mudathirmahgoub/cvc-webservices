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

import cvc.Contracts.RawResults;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Eric, Mingyu
 */

public class CvcClient
{
    private static final Logger log = Logger.getLogger(cvc.CvcClient.class);

    public CvcClient()
    {
        Logger.getLogger("org.apache.http").setLevel(org.apache.log4j.Level.OFF);
    }


    public static void cancelJob(String jobId)
    {
        if (CvcContext.runningTasks.containsKey(jobId))
        {
            Future<Void> future = CvcContext.runningTasks.get(jobId);
            future.cancel(true);
            CvcContext.runningTasks.remove(jobId);
            System.out.println("Job id " + jobId + " is canceled");
        }
    }

    public static RawResults getRawResults(String jobId) throws Exception
    {
        String resultsPath = CvcContext.jobsDirectory + "/" + jobId + "/" + Constants.RESULTS_FILE;
        String errorsPath = CvcContext.jobsDirectory + "/" + jobId + "/" + Constants.ERRORS_FILE;

        byte[] resultsBytes = Files.readAllBytes(Paths.get(resultsPath));
        byte[] errorsBytes = Files.readAllBytes(Paths.get(errorsPath));

        StringBuilder builder = new StringBuilder();
        builder.append(new String(resultsBytes, Charset.defaultCharset()));
        builder.append(new String(errorsBytes, Charset.defaultCharset()));

        RawResults results = new RawResults();
        results.data = builder.toString();

        // preprocessing
        // remove absolute path information for security reasons
        String absolutePath = Paths.get(CvcContext.jobsDirectory + "/" + jobId + "/" ).toAbsolutePath().toString();
        results.data = results.data.replace(absolutePath, "");
        return results;
    }

    /**
     * Attempts to create a new job
     *
     * @param jobId The unique Id of the job
     * @param code  Lustre code
     * @param args  cvc program arguments
     */
    public static Void createJob(String jobId, String code, List<String> args)
    {
        File jobDir = saveJob(jobId, code);

        // create a file to store the results
        File resultsFile = new File(jobDir, Constants.RESULTS_FILE);
        File errorsFile = new File(jobDir, Constants.ERRORS_FILE);

        String command = CvcContext.cvcCommand
                .replace("{0}", jobDir.getAbsolutePath())
                .replace("{1}", Constants.CODE_FILE);

        for (String argument : args)
        {
            command += " " + argument;
        }

        // handle the timeout argument
        if(!args.contains(Constants.timeoutArgument))
        {
            command += " " + Constants.timeoutArgument + " " + CvcContext.softTimeout;
        }

        System.out.println(command);

        try
        {
            // delete the contents of the files if they exist
            if(resultsFile.exists())
            {
                RandomAccessFile randomAccessFile = new RandomAccessFile(resultsFile,"rw");
                randomAccessFile.setLength(0);
            }

            if(errorsFile.exists())
            {
                RandomAccessFile randomAccessFile = new RandomAccessFile(errorsFile,"rw");
                randomAccessFile.setLength(0);
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            processBuilder.redirectOutput(resultsFile);
            processBuilder.redirectError(errorsFile);
            Process process = processBuilder.start();
            process.waitFor(CvcContext.hardTimeout, TimeUnit.MILLISECONDS);
            process.destroyForcibly();
            process.waitFor();
        }
        catch (Exception e)
        {
            log.error("createJob says " + e.getMessage(), e);
            try
            {

                FileWriter fileWriter = new FileWriter(resultsFile, true);

                Formatter formatter = new Formatter(fileWriter);

                formatter.format("%s\n", e.getMessage());

                formatter.close();
            }
            catch (IOException exception)
            {
                exception.printStackTrace();
            }
        }
        finally
        {
            // remove the task from running tasks
            if (CvcContext.runningTasks.containsKey(jobId))
            {
                CvcContext.runningTasks.remove(jobId);
            }
        }
        return null;
    }

    public static File saveJob(String jobId, String code)
    {
        // create a new directory for the job
        File jobDir = new File(CvcContext.jobsDirectory + "/" + jobId);
        if (!jobDir.exists())
        {
            jobDir.mkdir();
        }

        // create a file to store the Lustre code
        File inputFile = new File(jobDir, Constants.CODE_FILE);
        Util.writeToFile(code, inputFile);

        return jobDir;
    }
}
