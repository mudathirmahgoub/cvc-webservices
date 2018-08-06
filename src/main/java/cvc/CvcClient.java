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
import org.apache.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.lang.Runtime;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        String filePath = CvcContext.jobsDirectory + "/" +
                jobId +
                "/" + Constants.RESULTS_FILE;
        byte[] bytes = Files.readAllBytes(Paths.get(filePath));

        RawResults results = new RawResults();

        results.data = new String(bytes, Charset.defaultCharset());

        return results;
    }

    public static Object getObjectFromXmlElement(String element, Class<?> boundClass)
    {
        Object object = null;
        try
        {
            JAXBContext jaxbContext = JAXBContext.newInstance(boundClass);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(element);
            object = unmarshaller.unmarshal(reader);
        }
        catch (JAXBException e)
        {
            e.printStackTrace();
        }
        return object;
    }

    public static List<String> getElements(String data, String regex)
    {
        List<String> elements = new ArrayList<>();

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(data);

        while (matcher.find())
        {
            elements.add(matcher.group());
        }
        return elements;
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

        String command = CvcContext.cvcCommand
                .replace("{0}", jobDir.getAbsolutePath())
                .replace("{1}", Constants.CODE_FILE);

        for (String argument : args)
        {
            command += " " + argument;
        }

        System.out.println(command);

        try
        {
            // delete the contents of the file if it exists
            if(resultsFile.exists())
            {
                RandomAccessFile randomAccessFile = new RandomAccessFile(resultsFile,"rw");
                randomAccessFile.setLength(0);
            }

            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(command, new String[] {CvcContext.cvcPath});

            Scanner scanner = new Scanner(process.getInputStream());
            while (scanner.hasNext())
            {
                String line = scanner.nextLine();
                FileWriter fileWriter = new FileWriter(resultsFile, true);
                Formatter formatter = new Formatter(fileWriter);
                formatter.format("%s\n", line);
                formatter.close();
            }
        }
        catch (IOException e)
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
