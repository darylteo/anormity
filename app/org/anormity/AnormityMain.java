package org.anormity;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.LoaderClassPath;
import javassist.Modifier;
import javassist.NotFoundException;

class AnormityMain {
	
	private ClassPool _pool;
	
	public AnormityMain(ClassLoader loader){
		this._pool = new ClassPool();
		this._pool.appendClassPath(new LoaderClassPath(loader));		
	}
	public void anormitify(String className) throws Exception{
		CtClass clazz = _pool.get(className);
		CtField[] fields = clazz.getDeclaredFields();
				
		for (CtField field : fields){
			if (!Modifier.isPublic(field.getModifiers())){
				continue;
			}
			
			privatizeField(field,clazz);
			proxifyField(field, clazz);
		}
		
		clazz.toClass();
	}
	
	private void privatizeField(CtField field, CtClass declaringClass) throws CannotCompileException {				
		CtField flag = new CtField(field,declaringClass);
		String name = "__" + flag.getName();
		flag.setModifiers(Modifier.PRIVATE);
		flag.setName(name);		
		flag.setType(CtClass.booleanType);
		
		declaringClass.addField(flag);
	}
	
	private void proxifyField(CtField field, CtClass declaringClass) throws CannotCompileException, NotFoundException {
		generateMutator(field, declaringClass);
		generateAccessor(field, declaringClass);
	}
	
	private void generateMutator(CtField field, CtClass declaringClass) throws CannotCompileException, NotFoundException {
		/* turns camelCase to CamelCase */
		String fieldName = field.getName();
		String methodName = "set" + ("" +  fieldName.charAt(0)).toUpperCase() + fieldName.substring(1); 
	
		CtClass fieldType = field.getType();
		CtClass[] parameterTypes = new CtClass[]{ fieldType };

		/* Check for existing declared mutator */
		try{
			CtMethod existingMethod = declaringClass.getDeclaredMethod(methodName, parameterTypes);

			existingMethod.insertAfter(
				"__" + fieldName + " = true;"
			);
		}catch(NotFoundException e){
			
		}
	}
	
	private void generateAccessor(CtField field, CtClass declaringClass) throws CannotCompileException, NotFoundException {
		/* turns camelCase to CamelCase */
		String fieldName = field.getName();
		String methodName = "get" + ("" +  fieldName.charAt(0)).toUpperCase() + fieldName.substring(1); 
		
		CtClass fieldType = field.getType();
		CtClass[] parameterTypes = new CtClass[]{ };
		
		/* Check for existing declared mutator */
		try{
			CtMethod existingMethod = declaringClass.getDeclaredMethod(methodName, parameterTypes);

			existingMethod.insertAfter(
				"if (!__" + fieldName + "){"
				+ "throw new Exception(\"No Value Set For Field => " + declaringClass.getName() + "."+ fieldName + ":" + fieldType.getName() + "\");"
				+ "}"
			);	
		}catch(NotFoundException e){
		}
	}
}
