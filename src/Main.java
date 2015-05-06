import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class Main
{
	enum Command
	{
		INIT("init", "Initialize a binary_manager repository"),
		COMMIT("ci", "Commit the current directory state"),
		DIFF("diff", "Compare the current directory state with the previous one"),
		FIND_DUPLICATES("fdup", "Find duplicated files");

		public final String cmdName;
		public final String description;

		Command(String cmdName, String description)
		{
			this.cmdName = cmdName;
			this.description = description;
		}

		public static Command fromName(final String cmdName)
		{
			for (final Command command : values())
			{
				if (command.cmdName.equals(cmdName))
				{
					return command;
				}
			}
			return null;
		}
	}

	/**
	 * Construct Options.
	 */
	public static Options constructOptions()
	{
		Options options = new Options();
		options.addOption(createOption("v", "verbose", false, "Display details", false));
		options.addOption(createOption("m", "message", true, "Message to store with the state", false));
		return options;
	}

	public static Option createOption(String opt, String longOpt, boolean hasArg, String description, boolean required)
	{
		Option option = new Option(opt, longOpt, hasArg, description);
		option.setRequired(required);
		return option;
	}

	public static void main(String[] args) throws IOException
	{
		if (args.length < 1)
		{
			youMustSpecifyACommandToRun();
		}

		Command command = Command.fromName(args[0]);
		if (command == null)
		{
			youMustSpecifyACommandToRun();
		}

		CommandLineParser cmdLineGnuParser = new GnuParser();

		Options options = constructOptions();
		CommandLine commandLine;

		boolean verbose = false;
		String message = "";

		try
		{
			String[] actionArgs = Arrays.copyOfRange(args, 1, args.length);
			commandLine = cmdLineGnuParser.parse(options, actionArgs);
			if (commandLine.hasOption("h"))
			{
				printUsage();
				System.exit(0);
			}
			else
			{
				verbose = commandLine.hasOption('v');
				message = commandLine.getOptionValue('m', message);
			}
		}
		catch (Exception ex)
		{
			printUsage();
			System.exit(-1);
		}

		File baseDirectory = new File(".");
		File stateDir = new File(".bm/states");

		if (command == Command.INIT)
		{
			if (stateDir.exists())
			{
				System.out.println("binary_manager repository already exist");
				System.exit(0);
			}
		}
		else
		{
			if (!stateDir.exists())
			{
				System.out.println("binary_manager repository does not exist. Please run 'bm init' before.");
				System.exit(0);
			}
		}

		State previousState;
		State currentState;

		StateGenerator generator = new StateGenerator();
		StateManager manager = new StateManager(stateDir);
		StateComparator comparator = new StateComparator();
		DuplicateFinder finder = new DuplicateFinder();

		switch (command)
		{
			case INIT:
				stateDir.mkdirs();
				currentState = generator.generateState("Initial state", baseDirectory);
				comparator.compare(null, currentState, verbose);
				manager.createNewState(currentState);
				break;

			case COMMIT:
				previousState = manager.loadLastState();
				currentState = generator.generateState(message, baseDirectory);
				comparator.compare(previousState, currentState, verbose);
				manager.createNewState(currentState);
				break;

			case DIFF:
				previousState = manager.loadLastState();
				currentState = generator.generateState(message, baseDirectory);
				comparator.compare(previousState, currentState, verbose);
				break;

			case FIND_DUPLICATES:
				System.out.println("Searching for duplicated files");
				currentState = generator.generateState(message, baseDirectory);
				finder.findDuplicates(currentState, verbose);
				break;
		}
	}

	private static void youMustSpecifyACommandToRun()
	{
		System.out.println("You must specify the command to run");
		printUsage();
		System.exit(-1);
	}

	public static void printUsage()
	{
		System.out.println("");
		Options options = constructOptions();
		PrintWriter writer = new PrintWriter(System.out);
		HelpFormatter helpFormatter = new HelpFormatter();

		String usage = "\n  Available commands:\n";
		for (final Command command : Command.values())
		{
			usage += "- " + command.cmdName + ": " + command.description + "\n";
		}

		helpFormatter.printHelp(writer, 110, "binary_manager <command>", "\nManages binary files\n", options, 5, 3, usage, true);
		writer.flush();
		System.out.println("");
	}
}
