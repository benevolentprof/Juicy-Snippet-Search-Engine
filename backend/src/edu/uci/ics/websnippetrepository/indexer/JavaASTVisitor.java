package edu.uci.ics.websnippetrepository.indexer;

import java.util.LinkedList;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BlockComment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

/**
 * A storage and a visitor for Java Abstract Syntax Tree.
 * It will capture all the identifiers and store them into separate categories.
 * 
 * @author ptantiku
 */
public class JavaASTVisitor extends ASTVisitor {
	public String packageStr = "";
	public LinkedList<String> importsList = new LinkedList<String>();
	public LinkedList<String> extendsList= new LinkedList<String>();	//extends and implements
	public LinkedList<String> classesList = new LinkedList<String>();
	public LinkedList<String> classesUsedList = new LinkedList<String>();
	public LinkedList<String> methodsList = new LinkedList<String>();
	public LinkedList<String> methodsCalledList = new LinkedList<String>();
	public LinkedList<String> variablesList = new LinkedList<String>();
	public LinkedList<String> returnsList = new LinkedList<String>();
	public LinkedList<String> commentsList = new LinkedList<String>();
	
	/*------------------------PACKAGE----------------------------*/
	public boolean visit(PackageDeclaration declare){
		packageStr = declare.getName().getFullyQualifiedName();
		return true;
	}
	
	/*------------------------IMPORTS----------------------------*/
	public boolean visit(ImportDeclaration declare){
		importsList.add(declare.getName().getFullyQualifiedName());
		return true;
	}
	
	/*------------------------CLASSES----------------------------*/
	public boolean visit(TypeDeclaration declare){
		classesList.add(declare.getName().getFullyQualifiedName());
		
		if(declare.getSuperclassType()!=null){
			extendsList.add(declare.getSuperclassType().toString());
			classesUsedList.add(declare.getSuperclassType().toString());
		}
		
		if(declare.superInterfaceTypes().size()>0){
			for(Object iter:declare.superInterfaceTypes()){
				extendsList.add(iter.toString());
				classesUsedList.add(iter.toString());
			}
		}
		
		return true;
	}
	
	/*------------------------VARIABLES----------------------------*/
	public boolean visit(FieldDeclaration declare){
		classesUsedList.add(declare.getType().toString());
		return true;
	}
	
	public boolean visit(VariableDeclarationStatement declare){
		classesUsedList.add(declare.getType().toString());
		return true;
	}
	
	public boolean visit(VariableDeclarationFragment declare){
		variablesList.add(declare.getName().getFullyQualifiedName());
		return true;
	}
	
	public boolean visit(ClassInstanceCreation declare){
		classesUsedList.add(declare.getType().toString());
		return true;
	}
	
	/*------------------------METHODS----------------------------*/
	public boolean visit(MethodDeclaration declare){
		methodsList.add(declare.getName().getFullyQualifiedName());
		
		//return type as both return type and class use
		if(declare.getReturnType2()!=null){	//if return type is not void
			returnsList.add(declare.getReturnType2().toString());
			classesUsedList.add(declare.getReturnType2().toString());
		}
		return true;
	}
	
	public boolean visit(MethodInvocation declare){
		methodsCalledList.add(declare.getName().getFullyQualifiedName());
		return true;
	}
	
	/*------------------------COMMENTS----------------------------*/
	public boolean visit(LineComment declare){
		commentsList.add(declare.toString());
		return true;
	}
	
	public boolean visit(BlockComment declare){
		commentsList.add(declare.toString());
		return true;
	}
	
	public boolean visit(Javadoc declare){
		commentsList.add(declare.toString());
		return true;
	}
}
	