/*
 * Copyright (C) 2017, 2018, 2019, 2020 University of Illinois Board of 
 * Trustees.
 *
 * This file is part of bTools.
 *
 * bTools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * bTools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with bTools. If not, see http://www.gnu.org/licenses/.
 */

package edu.illinois.gernat.btools.common.parameters;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @version 0.12.0
 * @since 0.12.0
 * @author Tim Gernat
 */
// mapping strings:
// - mapping strings assign arguments to parameters  
// - parameter and argument must be separated by the = character
// - it is illegal to have more than one = character in a mapping string
// - characters to the left of the = character are the parameter
// - characters to the right of the = character are the argument
// - leading and trailing whitespace characters will be stripped from the 
//   parameter and the argument
// - arguments containing whitespace must be enclosed in quotation marks
// - quotation marks enclosing an arguments will be removed
// - parameter and argument must be non-empty after stripping whitespace and
//   removing quotation marks
//
// configuration files:
// - are plain text files contain mapping lines, comment lines, and empty lines
// - comment lines begin with a # character and will be ignored
// - mapping lines contain at least one = character and will be used to map 
//   an argument to a parameter
// - empty lines contain only whitespace characters and will be ignored
public enum Parameters
{

	INSTANCE;
	
	private static final int PARAMETER = 0;
	
	private static final int ARGUMENT = 1;
	
	private static final String CONFIGURATION_FILE_KEY = "configuration.file";
	
	private HashMap<String, String> arguments;
	
	private Parameters()
	{
	}
	
	public void initialize(String[] commandLineArguments) throws IOException
	{
		
		// parse command line arguments into local map 
		HashMap<String, String> arguments = new HashMap<String, String>();
		for (String argument : commandLineArguments) 
		{
			Tuple<String, String> keyValuePair = parseMappingString(argument);
			arguments.put(keyValuePair.a, keyValuePair.b);			
		}
		
		// if a configuration file is specified on the command line, read 
		// arguments from configuration file into global map
		this.arguments = new HashMap<String, String>();
		if (arguments.containsKey(CONFIGURATION_FILE_KEY))
		{
			BufferedReader reader = new BufferedReader(new FileReader(arguments.get(CONFIGURATION_FILE_KEY)));		
			while (reader.ready())
			{
				String line = reader.readLine().trim();
				if ((line.startsWith("#")) || (line.isEmpty())) continue;
				else if (line.indexOf('=') != -1) 
				{
					Tuple<String, String> keyValuePair = parseMappingString(line);
					this.arguments.put(keyValuePair.a, keyValuePair.b);
				}
				else 
				{
					reader.close();
					throw new IllegalStateException();
				}
			}
			reader.close();
		}
		
		// override configuration file arguments with command line arguments
		for (String parameter : arguments.keySet()) this.arguments.put(parameter, arguments.get(parameter));
		
	}

	public boolean exists(String parameter)
	{
		return arguments.get(parameter) != null;
	}

	public void set(String parameter, String argument)
	{
		arguments.put(parameter, argument);
	}
	
	public String getString(String parameter)
	{
		return getArgument(parameter);
	}

	public int getInteger(String parameter)
	{
		String argument = getArgument(parameter);
		if (argument.equals("-Infinity")) return Integer.MIN_VALUE;
		else if (argument.equals("+Infinity")) return Integer.MAX_VALUE;
		else return Integer.parseInt(argument);
	}
	
	public boolean getBoolean(String parameter)
	{
		return Boolean.parseBoolean(getArgument(parameter)) || (getInteger(parameter) == 1);
	}

	public double getDouble(String parameter)
	{
		String argument = getArgument(parameter);
		if (argument.equals("-Infinity")) return Double.MIN_VALUE;
		else if (argument.equals("+Infinity")) return Double.MAX_VALUE;
		return Double.parseDouble(argument);
	}

	public long getLong(String parameter)
	{
		String argument = getArgument(parameter);
		if (argument.equals("-Infinity")) return Long.MIN_VALUE;
		else if (argument.equals("+Infinity")) return Long.MAX_VALUE;
		return Long.parseLong(argument);
	}
	
	//BUG: this is unable to handle negative integers in list 
	public List<Integer> getIntegerList(String parameter)
	{
		List<Integer> list = new ArrayList<Integer>();
		String[] listElements = getArgument(parameter).split(",");
		for (String element : listElements)
		{
			if (element.indexOf('-') == -1) list.add(Integer.parseInt(element));
			else
			{
				String[] range = element.split("-");
				int bound1 = Integer.parseInt(range[0]);
				int bound2 = Integer.parseInt(range[1]);
				int min = Math.min(bound1, bound2);
				int max = Math.max(bound1, bound2);
				for (int i = min; i <= max; i++) list.add(i);
			}
		}
		return list;
	}
	
	private Tuple<String, String> parseMappingString(String s)
	{
		int separatorIndex = s.indexOf('=');
		if (separatorIndex == -1) throw new IllegalArgumentException("Parameter: " + s);
		if (s.indexOf('=', separatorIndex + 1) != -1) throw new IllegalArgumentException("Parameter: " + s);
		String[] tokens = s.split("=");
		String parameter = parseToken(tokens[PARAMETER]);
		String argument = parseToken(tokens[ARGUMENT]);
		return Tuple.of(parameter, argument);		
	}
	
	private String parseToken(String token)
	{
		token = token.trim();
		if (token.startsWith("\"") && token.endsWith("\"")) 
		{
			token = token.substring(1, token.length() - 1);
			token.trim();
		}
		if (token.isEmpty()) throw new IllegalArgumentException();
		return token;
	}

	private String getArgument(String parameter)
	{
		String argument = arguments.get(parameter);
		if (argument == null) throw new IllegalStateException("Unknown parameter: " + parameter);
		return argument;
	}
	
	private static void showVersionAndCopyright() 
	{
		System.out.println("Parameter Reader (bTools) 0.14.0");
		System.out.println("Copyright (C) 2017, 2018, 2019, 2020 University of Illinois Board of Trustees");
		System.out.println("License AGPLv3+: GNU AGPL version 3 or later <http://www.gnu.org/licenses/>");
		System.out.println("This is free software: you are free to change and redistribute it.");
		System.out.println("There is NO WARRANTY, to the extent permitted by law.");
	}

	private static void showUsageInformation() 
	{
		System.out.println("Usage: java -jar read_parameters.jar CONFIGURATION_FILE PARAMETER_NAME...");
		System.out.println("Read parameter values from configuration file.");
	}
	
	public static void main(String[] args) throws IOException
	{
	
		// show version, copyright, and usage information if no arguments were 
		// given on the command line 
		if (args.length == 0) 
		{
			showVersionAndCopyright();
			System.out.println();
			showUsageInformation();		
			System.exit(1);
		}

		// initialize parameters instance from configuration file
		Parameters parameters = Parameters.INSTANCE;
		String configurationFileArgument[] = { args[0] };
		parameters.initialize(configurationFileArgument);

		// for each additional parameter argument get argument from 
		// configuration file 
		for (int i = 1; i < args.length; i++) 
		{
			if (i > 1) System.out.print(" ");
			System.out.print(parameters.getArgument(args[i])); //TODO add quotation marks to strings that include space character
		}
		System.out.println();
		
	}
	
}
