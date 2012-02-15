package org.anormous;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.net.*;

import play.Play;
import play.Plugin;
import play.api.Application;
import javassist.*;


public class AnormousPlugin extends Plugin{
	
	private ClassPool _pool;
	private Application _app;
	public AnormousPlugin(Application application){
		this._app = application;
		
		this._pool = new ClassPool();
		this._pool.appendClassPath(new LoaderClassPath(application.classloader()));
	}
	public void onStart(){		
		
		try {
			Set<String> types = Play.application().getTypesAnnotatedWith("models", org.anormous.AnormousEntity.class);
			
			for (String type : types){
				System.out.println("Anormous: Enhancing type " + type);
				anormify(type);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	public void anormify(String className) throws Exception{
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
	
	
	
	/**
	 * Scans all classes accessible from the context class loader which belong
	 * to the given package and subpackages.
	 * 
	 * @param packageName
	 *            The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private Iterable<Class<?>> findClasses(String packageName) throws ClassNotFoundException, IOException
	{
	    ClassLoader classLoader = this._app.classloader();
	    String path = packageName.replace('.', '/');
	    Enumeration<URL> resources = classLoader.getResources(path);
	    List<File> dirs = new ArrayList<File>();
	    while (resources.hasMoreElements())
	    {
	        URL resource = resources.nextElement();
	        dirs.add(new File(resource.getFile()));
	    }
	    List<Class<?>> classes = new ArrayList<Class<?>>();
	    for (File directory : dirs)
	    {
	        classes.addAll(findClasses(directory, packageName));
	    }

	    return classes;
	}
	/**
	 * Recursive method used to find all classes in a given directory and
	 * subdirs.
	 * 
	 * @param directory
	 *            The base directory
	 * @param packageName
	 *            The package name for classes found inside the base directory
	 * @return The classes
	 * @throws ClassNotFoundException
	 */
	private List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException
	{
	    List<Class<?>> classes = new ArrayList<Class<?>>();
	    if (!directory.exists())
	    {
	        return classes;
	    }
	    File[] files = directory.listFiles();
	    for (File file : files)
	    {
	        if (file.isDirectory())
	        {
	            classes.addAll(findClasses(file, packageName + "." + file.getName()));
	        }
	        else if (file.getName().endsWith(".class"))
	        {
	            classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
	        }
	    }
	    return classes;
	}

}
