
package edu.uci.ics.websnippetrepository.indexer;

import java.util.LinkedList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;

/**
 * Java Source Code Parser, to parse sourcecode into list of identifiers according to their categories:class,method,variable,comment.
 * 
 * @author ptantiku
 *
 */
public class JavaCodeParser {

	/**
	 * Use single-ton ASTParser as an engine to parse source code
	 */
	private static ASTParser parser = ASTParser.newParser(AST.JLS3);// handles JDK 1.0,
															// 1.1, 1.2, 1.3,
															// 1.4, 1.5, 1.6
	
	/**
	 * Parse all java elements in sourcecode into storage in JavaASTVisitor
	 * @param sourceCodeString	Java source code, either CompilationUnit(whole class file) or Statements(code snippet)
	 * @return	all elements that has been parsed.
	 */
	public static synchronized JavaASTVisitor parseJavaCode(String sourceCodeString){
		ASTNode node = null;

		// try parsing as it's a class first
		parser.setSource(sourceCodeString.toCharArray());
		parser.setResolveBindings(false);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		node = parser.createAST(null);

		// if the parsed node gets some error, it means the source code is not CompilationUnit  
		// try parsing as Statements
		if (node.getFlags() > ASTNode.ORIGINAL) {
			parser.setSource(sourceCodeString.toCharArray());
			parser.setResolveBindings(false);
			parser.setKind(ASTParser.K_STATEMENTS);
			node = parser.createAST(null);
		} 

		//parse all java elements in source code into storage in JavaASTVisitor
		JavaASTVisitor result = new JavaASTVisitor();
		node.accept(result);
		
		return result;
	}

	/**
	 * Splitting the input term into several lower-case words based on camel-case
	 * @param term	any keyword term, e.g. class name, method name, variable name
	 * @return	List of words splitted from the input term
	 */
	public static LinkedList<String> camelcaseSplit(String term){
		LinkedList<String> result = new LinkedList<String>();
	
		int startPos=0;
		final int LOWERCASE_STATE = 0;
		final int UPPERCASE_STATE = 1;
		final int NUMBER_STATE = 2;
		final int OTHERS_STATE = 3;
		
		int state=OTHERS_STATE;	
		
		//starting by 0, ignore the first character which usually can be either lower or upper case
		for(int i=0;i<term.length();i++){
			int newState=0;
			if(Character.isDigit(term.charAt(i)))
				newState = NUMBER_STATE;
			else if(Character.isLetter(term.charAt(i))){
				if(Character.isLowerCase(term.charAt(i)))
					newState = LOWERCASE_STATE;
				else if(Character.isUpperCase(term.charAt(i)))
					newState = UPPERCASE_STATE;
			}else
				newState = OTHERS_STATE;
			
			//if this is first character, skip
			if(i==0){
				state = newState;
				continue;
			}
			
			if(state!=newState){
				if( //if not changing from upper case to lower case, which does nothing
					!(state==UPPERCASE_STATE && newState==LOWERCASE_STATE)){
					//other cases, do the split
					
					if(state!=OTHERS_STATE)	//if not change from _ to alnum
						result.add(term.substring(startPos, i).toLowerCase());
					startPos = i;
				}
			}
			state = newState;
		}
		
		//for the final term
		if(startPos!=term.length() && startPos!=0 && state!=OTHERS_STATE)
			result.add(term.substring(startPos).toLowerCase());
	
		return result;
	}
	
}
