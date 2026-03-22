package rs.ac.bg.etf.pp1;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.analysis.DumpSymbolTableVisitorWrapper;
import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.log.Log4JUtils;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.Scope;
import rs.etf.pp1.symboltable.visitors.SymbolTableVisitor;

public class Compiler {
	static Logger log = Logger.getLogger(Compiler.class);
	static {
		Log4JUtils.instance().configure();
	}

	public static void main(String[] args) throws IOException {
		try {

			log.setLevel(Level.INFO);

			if (args.length != 2) {
				log.error("Expected exactly 2 arguments: [Input_file] [Output_file]");
				System.exit(1);
			}

			String inputFile = args[0];
			String outputFile = args[1];

			compile(inputFile, outputFile);

		} catch (Exception e) {
			log.error("\nCompiler crashed: " + e.getMessage());
			System.exit(1);
		}
	}

	static private void compile(String inputFile, String outputFile) throws Exception {
		Reader reader = new FileReader(inputFile);

		log.info("Starting lexical analysis");
		Lexer lexer = new Lexer(reader);

		log.info("Starting syntax analysis");
		Parser parser = new Parser(lexer);
		if (parser.detectedError) {
			log.error("Error during syntax analysis");
			System.exit(1);
		}

		Program prog = (Program) parser.parse().value;
		if (parser.detectedError) {
			log.error("Error during syntax analysis");
			System.exit(1);
		}

		log.debug(prog.toString("	"));

		log.info("Starting semantic analysis");
		SemanticAnalyzer analyzer = new SemanticAnalyzer(prog);
		analyzer.execute();
		if (analyzer.getErrorsFound()) {
			log.error("Error during semantic analysis");
			System.exit(1);
		}

		log.debug(Compiler.tsdump());

		File out = new File(outputFile);
		if (out.exists())
			out.delete();

		log.info("Starting code generation");
		int globalVariableCnt = analyzer.getGlobalVariableCnt();
		CodeGenerator generator = new CodeGenerator(prog, globalVariableCnt, out);
		generator.execute();

		if (generator.getErrorsFound()) {
			log.error("Error during code generation");
			System.exit(1);
		}

		log.info("Successfully finished the compiler");
	}

	private static String tsdump() {
		StringBuilder sb = new StringBuilder("\n==================== SYMBOL TABLE DUMP ========================\n");
		SymbolTableVisitor stv = new DumpSymbolTableVisitorWrapper();
		for (Scope s = Tab.currentScope; s != null; s = s.getOuter()) {
			s.accept(stv);
		}
		sb.append(stv.getOutput());
		return sb.toString();
	}
}
