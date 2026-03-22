package rs.ac.bg.etf.pp1;
import java_cup.runtime.Symbol;

%%
%class Lexer

%unicode

%{
	boolean VERBOSE = false;

	private Symbol	new_symbol(String name, int type){
		if(VERBOSE) System.out.println(name);
		return new Symbol(type,yyline+1,yycolumn);
	}
	private Symbol new_symbol(String name, int type,Object value){
		if(VERBOSE) System.out.println(name + " " + String.valueOf(value));
		return new Symbol(type,yyline+1,yycolumn,value);
	}
%}

%line
%column
%cup

%%
/* Keywords */
"program"      { return new_symbol("PROGRAM", sym.PROGRAM, yytext()); }
"enum"         { return new_symbol("ENUM", sym.ENUM, yytext()); }
"const"        { return new_symbol("CONST", sym.CONST, yytext()); }
"new"          { return new_symbol("NEW", sym.NEW, yytext()); }
"print"        { return new_symbol("PRINT", sym.PRINT, yytext()); }
"read"         { return new_symbol("READ", sym.READ, yytext()); }
"return"       { return new_symbol("RETURN", sym.RETURN, yytext()); }
"void"         { return new_symbol("VOID", sym.VOID, yytext()); }
"length"       { return new_symbol("LENGTH", sym.LENGTH, yytext()); }
"main"         { return new_symbol("MAIN", sym.MAIN, yytext()); }



"true"|"false"			  	{ 
								boolean value = Boolean.parseBoolean(yytext());
								return new_symbol("BOOL_CONST", sym.BOOL_CONST, value? 1:0); 
							}


/* Identifiers */
[a-zA-Z][a-zA-Z0-9_]*      	{ return new_symbol("IDENTIFIER", sym.IDENTIFIER, yytext()); }

/* Numbers */
[0-9]+                     	{ 
                                try {
                                    int num = Integer.parseInt(yytext());
                                    return new_symbol("NUM_CONST", sym.NUM_CONST, num);
                                } catch (NumberFormatException e) {
                                    System.err.println("Invalid number: " + yytext() + " at line " + yyline + ", column " + yycolumn);
                                    return new_symbol("error", sym.ERROR);
                                }
                            }

/* Char Literals */
"'"[^\n]"'"		           	{ 
                                String str = yytext();
                                return new_symbol("CHAR_CONST", sym.CHAR_CONST, Character.valueOf(str.charAt(1)));
                            }


/* Symbols */
"("      { return new_symbol("LPAREN", sym.LPAREN, yytext()); }
")"      { return new_symbol("RPAREN", sym.RPAREN, yytext()); }
"{"      { return new_symbol("LBRACE", sym.LBRACE, yytext()); }
"}"      { return new_symbol("RBRACE", sym.RBRACE, yytext()); }
"["      { return new_symbol("LBRACKET", sym.LBRACKET, yytext()); }
"]"      { return new_symbol("RBRACKET", sym.RBRACKET, yytext()); }
","      { return new_symbol("COMMA", sym.COMMA, yytext()); }
"."      { return new_symbol("DOT", sym.DOT, yytext()); }
";"      { return new_symbol("SEMICOLON", sym.SEMICOLON, yytext()); }
":"      { return new_symbol("COLON", sym.COLON, yytext()); }
"?"      { return new_symbol("QUESTIONMARK", sym.QUESTIONMARK, yytext()); }
"@"      { return new_symbol("AT", sym.AT, yytext()); }

/* Operators */
"==" | "!=" | "<=" | ">=" | "<" | ">"   { return new_symbol("RELOP", sym.RELOP, yytext()); }
"+" | "-"                               { return new_symbol("ADDOP", sym.ADDOP, yytext()); }
"*" | "/" | "%"                         { return new_symbol("MULOP", sym.MULOP, yytext()); }
"++" | "--"                             { return new_symbol("INCDEC", sym.INCDEC, yytext()); }
"="                                     { return new_symbol("ASSIGN", sym.ASSIGN, yytext()); }


/* Comments */
"//" [^\n]*             	{}


" " 						{ return new_symbol("", sym.SPACE, " "); }
"\t" 						{}
"\r\n" 						{ return new_symbol("", sym.NEWLINE, "\n"); }
"\r" 					    {}
"\f" 						{}
"\n" 						{ return new_symbol("", sym.NEWLINE, "\n"); }


. 							{ System.err.println("LEXICAL ERROR '"+yytext()+"'  Line: "+(yyline+1)+" Column: " + (yycolumn+1)); System.exit(1); }