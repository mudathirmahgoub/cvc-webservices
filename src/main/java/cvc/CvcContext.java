package cvc;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.*;

import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * This class contains methods invoked when the system is first set up and shut down
 *
 * @author Eric
 */

public class CvcContext implements ServletContextListener
{
    private static final ScheduledExecutorService taskScheduler = Executors.newScheduledThreadPool(1);

    private static ExecutorService cvcExecutorService;

    public static ExecutorService getCvcExecutorService()
    {
        return cvcExecutorService;
    }

    public static String cvcPath;
    public static String cvcCommand;
    public static int maxThreads;

    public static HashMap<String, Argument> cvcArguments;

    public static String jobsDirectory;
    public static String examplesDirectory;

    public static String softTimeout; // milliseconds
    public static int hardTimeout; // milliseconds

    public static final ConcurrentMap<String, Future<Void>> runningTasks =
            new ConcurrentHashMap<>();

    /**
     * When the application starts, this method is called. Perform any initializations here
     */
    @Override
    public void contextInitialized(ServletContextEvent event)
    {
        LoadConfigurations();
        LoadArguments();

        examplesDirectory = getResourceDirectory(Constants.examplesDir);

        /*  Create a task that deletes job logs older than 3 days */
        final Runnable clearJobLogTask = new RobustRunnable("clearJobLogTask")
        {
            @Override
            protected void dorun()
            {
                Util.clearOldFiles(new File(jobsDirectory).getAbsolutePath(), 3);
            }
        };
        taskScheduler.scheduleAtFixedRate(clearJobLogTask, 0, 1, TimeUnit.DAYS);
    }

    private void LoadConfigurations()
    {
        InputStream inputStream =
                CvcContext.class.getResourceAsStream("/configurations.json");
        /* load the configuration properties */
        try
        {
            String json = IOUtils.toString(inputStream, Charset.defaultCharset());
            inputStream.close();

            ObjectMapper mapper = new ObjectMapper();
            HashMap<String, String> configurations = mapper.readValue(json,
                    new TypeReference<HashMap<String, String>>(){});

            cvcPath= configurations.get("cvcPath");
            jobsDirectory = configurations.get("jobsDirectory");
            cvcCommand = configurations.get("cvcCommand");
            maxThreads = Integer.parseInt(configurations.get("maxThreads"));
            cvcExecutorService = newFixedThreadPool(maxThreads);

            softTimeout = configurations.get("softTimeout");
            hardTimeout = Integer.parseInt(configurations.get("hardTimeout"));
        }
        catch (IOException exception)
        {
            exception.printStackTrace();
        }
    }

    private void LoadArguments()
    {
        InputStream inputStream =
                CvcContext.class.getResourceAsStream("/cvcArguments.json");
        try
        {
            String json = IOUtils.toString(inputStream, Charset.defaultCharset());
            inputStream.close();

            ObjectMapper mapper = new ObjectMapper();
            cvcArguments = mapper.readValue(json,
                    new TypeReference<HashMap<String, Argument>>(){});
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }
    @Override
    public void contextDestroyed(ServletContextEvent event)
    {
        try
        {
            cvcExecutorService.shutdown();
            taskScheduler.shutdown();
        }
        catch (Exception e)
        {
        }
    }

    /**
     * @return a string that represents the path the resource directory on the server
     */

    public static String getResourceDirectory(String localPath)
    {
        String directory = RESTServices.class.getProtectionDomain().getCodeSource().getLocation().getPath() +
                localPath;

        directory = directory.replace("%20", " ");
        if (directory.contains(":/"))
        {
            // in windows, remove the leading "/"
            directory = directory.substring(1);
        }
        return directory;
    }
}
