package org.anormity;

import java.util.*;
import java.io.File;
import java.io.IOException;
import java.net.*;

import play.Play;
import play.Plugin;
import play.api.Application;
import javassist.*;


public class AnormityPlugin extends Plugin{
	
	public AnormityPlugin(Application application){
	}
	public void onStart(){		
		
		AnormityMain main = new AnormityMain(Play.application().classloader());
		
		try {
			Set<String> types = Play.application().getTypesAnnotatedWith("models", org.anormity.Anormity.class);
			
			for (String type : types){
				System.out.println("Anormous: Enhancing type " + type);
				main.anormitify(type);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
