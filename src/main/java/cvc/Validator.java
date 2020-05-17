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

/**
 * This class is responsible for validating incoming job creation requests.
 */

import java.util.List;


public class Validator {

	/**
	 * Determines whether the given string represents a valid integer
	 * @param str The string to check
	 * @return True if valid, false otherwise.
	 * @author Eric Burns
	 */
	
	public static boolean isValidPosInteger(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Determines whether the given string is a valid boolean.
	 * @param str
	 * @return True if it is a valid boolean, false otherwise
	 */
	
	public static boolean isValidBoolean(String str) {
		str=str.toLowerCase();
		return (str.equals("true") || str.equals("false"));
	}
	/**
	 * Determines whether the given string is a valid double.
	 * @param dbl
	 * @return True if it is a valid double, false otherwise
	 */
	
	public static boolean isValidDouble(String dbl) {
		try {
			Double.parseDouble(dbl);
			return true;
		} catch (Exception e) {
			return false;
		}
		
	}
	
	
	/**
	 * Checks to see if the value of an argument is valid given the constraints specified
	 * in config.xml. Both the type and value given must match what is given in CvcArguments.json
	 * @param name The name of the parameter
	 * @param value The given argument
	 * @return True if valid, false otherwise
	 */
	
	public static boolean isArgValid(String name, String value)
	{
		// invalid argument
		if(! CvcContext.cvcArguments.containsKey(name))
		{
			return false;
		}

		// get the argument
		Argument argument = CvcContext.cvcArguments.get(name);

		// flag argument
		if(argument.type == null)
		{
			return true;
		}

		String type= argument.type;

		if (type.equals("int"))
		{
			if (!isValidPosInteger(value))
			{
				// the value of an int type must actually be an int
				return false;
			}
		}
		else if (type.equals("boolean"))
		{
			if (!isValidBoolean(value))
			{
				// must be a bool if that is what the type says
				return false;
			}
		}
		else if (type.equals("float"))
		{
			if (!isValidDouble(value))
			{
				//must be a valid double
				return false;
			}
		}
		else if (type.equals("string"))
		{
			//if we have a dropdown type, make sure that the value sent by the user is actually a real option
			if (Util.containsIgnoreCase(argument.allowedValues,value))
			{
				return true;
			}
			else
			{
				return false;
			}
		}

		// if we have a numeric type, we need to see if the given value conforms to limits min and max
		if (type.equals("int") || type.equals("float"))
		{
			float val = Float.parseFloat(value);
			if (val < argument.min || val > argument.max)
			{
				return false;
			}
		}
		return true;
	}
}
