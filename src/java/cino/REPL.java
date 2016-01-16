package cino;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class REPL {

	public static void main(String[] args) throws IOException {
		for (int i = 0; i < args.length; i++) {
			VM.run(Files.readAllBytes(Paths.get(args[i])));
		}
		if (args.length > 0)
			return;
		Scanner in = new Scanner(System.in);
		System.out.println("CINO v1");
		while (true) {
			System.out.print("$ ");
			VM.run(in.nextLine().getBytes());
		}
	}

}
