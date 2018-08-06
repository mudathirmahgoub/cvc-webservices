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
/**
 * This class contains some generic utility functions.
 */

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Collection;


public class Util
{
    private static final Logger log = Logger.getLogger(Util.class);

    /**
     * Deletes all files in the given directory that are as old as, or older than the specified number of days
     *
     * @param directory The directory to clear old files out of (non-recursive)
     * @param daysAgo   Files older than this many days ago will be deleted
     *                  Function originally designed for the StarExec project
     */
    public static void clearOldFiles(String directory, int daysAgo)
    {
        try
        {
            File dir = new File(directory);

            if (!dir.exists())
            {
                return;
            }

            // Subtract days from the current time
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -daysAgo);


            // Remove them all
            for (File file : dir.listFiles())
            {
                if (!file.getName().startsWith(Constants.tempPrefix))
                {
                    continue; //don't delete permanent files
                }
                if (file.lastModified() < calendar.getTimeInMillis())
                {
                    System.out.println(file.getAbsolutePath());
                    FileUtils.deleteQuietly(file);
                }
            }

        }
        catch (Exception e)
        {
            //log.warn(e.getMessage(), e);
        }
    }

    /**
     * Checks if the given string is contained in the given array,
     * regardless of case
     *
     * @param arr The array to search through
     * @param str The string to match
     * @return True if it is contained, false otherwise
     */

    public static boolean containsIgnoreCase(String[] arr, String str)
    {
        for (String s : arr)
        {
            if (str.equalsIgnoreCase(s))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given string is contained in the given array,
     * regardless of case
     *
     * @param arr The array to search through
     * @param str The string to match
     * @return True if it is contained, false otherwise
     */

    public static boolean containsIgnoreCase(Collection<String> arr, String str)
    {
        for (String s : arr)
        {
            if (str.equalsIgnoreCase(s))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes the given string to the given file
     *
     * @param str
     * @param file
     * @return
     */
    public static boolean writeToFile(String str, File file)
    {
        try
        {
            FileWriter writer = new FileWriter(file);
            BufferedWriter w = new BufferedWriter(writer);
            w.write(str);
            w.flush();
            w.close();
            writer.close();
            return true;
        }
        catch (Exception e)
        {
            log.error("writeToFile says " + e.getMessage(), e);
        }
        return false;
    }

    /**
     * Checks to see if a string is null or empty
     *
     * @param s
     * @return Function originally designed for the StarExec project
     */
    public static boolean isNullOrEmpty(String s)
    {
        return (s == null || s.trim().length() <= 0);
    }

}

