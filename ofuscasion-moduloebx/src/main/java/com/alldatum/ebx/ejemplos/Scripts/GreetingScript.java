package com.alldatum.ebx.ejemplos.Scripts;

import com.orchestranetworks.service.OperationException;
import com.orchestranetworks.workflow.ScriptTaskBean;
import com.orchestranetworks.workflow.ScriptTaskBeanContext;

public class GreetingScript extends ScriptTaskBean
{
	private String name;
	private String age;
	
	private String greeting;
	
	
	private String tellingName;
	
	private String tellingAge;
	

	public String getGreeting() {
		return greeting;
	}




	public void setName(String name) {
		this.name = name;
	}




	public void setAge(String age) {
		this.age = age;
	}



	public void generateGreeting()
	{
		this.tellingName="Hola yo soy "+name;
		this.tellingAge= "tengo "+age;
		this.greeting=tellingName+" y "+tellingAge;
		
	}
	

	@Override
	public void executeScript(ScriptTaskBeanContext arg0) throws OperationException {
		// TODO Auto-generated method stub
		generateGreeting();
	}
    
}
