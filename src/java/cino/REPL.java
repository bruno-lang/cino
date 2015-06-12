package cino;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class REPL {

	public static void main(String[] args) throws IOException {
		if (args.length > 0) {
			VM.run(Files.readAllBytes(Paths.get(args[0])));
			return;
		}
		Scanner in = new Scanner(System.in);
		System.out.println("cino v0");
		while (true) {
			System.out.print("$ ");
			VM.run(in.nextLine().getBytes());
		}
	}
	
	// findings:
	// maybe , for dup? a^b+ => a,b+
	// li is better index than ri
	// save l/r in ws that become "outside" of the current LR window when li is moved
	// use channels to provide console in/out stream? how to read until X
	// how to to a table switch? no instruction to go forward by value. how to validate that? by make it static declared so that the relative jumps are filled in by the init loop of a program.
	// @ ... , => ( ... )
}
