package com.alldatum.ebx.ejemplos.Scripts;
import com.alldatum.ebx.;
import com.onwbp.adaptation.Adaptation;
import com.onwbp.adaptation.AdaptationHome;
import com.onwbp.adaptation.AdaptationName;
import com.onwbp.adaptation.AdaptationTable;
import com.onwbp.adaptation.Request;
import com.onwbp.adaptation.RequestResult;
import com.onwbp.adaptation.XPathFilter;
import com.orchestranetworks.instance.HomeKey;
import com.orchestranetworks.instance.Repository;
import com.orchestranetworks.schema.Path;
import com.orchestranetworks.service.OperationException;
import com.orchestranetworks.service.Procedure;
import com.orchestranetworks.service.ProcedureContext;
import com.orchestranetworks.service.ProcedureResult;
import com.orchestranetworks.service.ProgrammaticService;
import com.orchestranetworks.service.Session;
import com.orchestranetworks.service.ValueContextForUpdate;
import com.orchestranetworks.workflow.ScriptTaskBean;
import com.orchestranetworks.workflow.ScriptTaskBeanContext;

public class RunLlmRequest extends  ScriptTaskBean
{
    //Datos del llm
	private String prompt;
	private String modeloLLm;
	private String temperatura;
	private String tokens;
	
	
	
	//Datos del origen del valor
	private String dataspaceOrigen;
    private String datasetOrigen;
    private String pathtoTablaOrigen;
    private String pathtoColumnaOrigen;
	
    //private String idRegistro; Previamente ahora es con xPath
    private String xPath;
    private String xparameters;
    private String values;
    //Datos del destino en el que se almacenara el valor estendarizado
    private String dataspaceDestino;
    private String datasetDestino;
    private String pathtoTablaDestino;
    private String pathtoColumnaDestino;
    
    private String processedRows;
    //Setters
    
    
    
    public void setDataspaceOrigen(String dataspaceOrigen) {
		this.dataspaceOrigen = dataspaceOrigen;
	}


	public void setModeloLLm(String modeloLLm) {
		this.modeloLLm = modeloLLm;
	}


	public void setTemperatura(String temperatura) {
		this.temperatura = temperatura;
	}


	public void setTokens(String tokens) {
		this.tokens = tokens;
	}


	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}


	public void setDatasetOrigen(String datasetOrigen) {
		this.datasetOrigen = datasetOrigen;
	}


	public void setPathtoTablaOrigen(String pathtoTablaOrigen) {
		this.pathtoTablaOrigen = pathtoTablaOrigen;
	}


	public void setPathtoColumnaOrigen(String pathtoColumnaOrigen) {
		this.pathtoColumnaOrigen = pathtoColumnaOrigen;
	}


	public void setDataspaceDestino(String dataspaceDestino) {
		this.dataspaceDestino = dataspaceDestino;
	}


	public void setDatasetDestino(String datasetDestino) {
		this.datasetDestino = datasetDestino;
	}


	public void setPathtoTablaDestino(String pathtoTablaDestino) {
		this.pathtoTablaDestino = pathtoTablaDestino;
	}


	public void setPathtoColumnaDestino(String pathtoColumnaDestino) {
		this.pathtoColumnaDestino = pathtoColumnaDestino;
	}

    
    
	/*
	public void setIdRegistro(String idRegistro) {
		this.idRegistro = idRegistro;
	}
     */

	public void setXPath(String xPath) {
		this.xPath = xPath;
	}


	public void setXparameters(String xPathParameters) {
		this.xparameters = xPathParameters;
	}


	public void setValues(String values) {
		this.values = values;
	}

	
	
	//getters
	public String getProcessedRows() {
		return processedRows;
	}


	public void setProcessedRows(String processedRows) {
		this.processedRows = processedRows;
	}

	

	@Override
	public void executeScript(ScriptTaskBeanContext scriptContext) throws OperationException 
	{
	   
		final Session sesionActual = scriptContext.getSession();
		final Repository repositorio = scriptContext.getRepository();
		
		final AdaptationHome originDataspace= toDataSpace(repositorio, this.dataspaceOrigen);
		final Adaptation originDataset =  toDataset(originDataspace, this.datasetOrigen);
		final AdaptationTable originTabla = toTable(originDataset, this.pathtoTablaOrigen);
		
		final Path caminoColumnaOrigen = Path.parse(this.pathtoColumnaOrigen);
		
		final AdaptationHome destinyDataspace= toDataSpace(repositorio, this.dataspaceDestino);
		final Adaptation destinyDataset =  toDataset(originDataspace, this.datasetDestino);
		final AdaptationTable destinyTabla = toTable(destinyDataset, this.pathtoTablaDestino);
		
		final Path caminoColumnaDestino = Path.parse(this.pathtoColumnaDestino);
		
		String valorAE=obtenerValorAEstandarizar(originDataspace, originTabla,this.xPath,this.xparameters ,this.values,caminoColumnaOrigen, sesionActual);
		generarTransaccionLLM(destinyDataspace, destinyTabla, caminoColumnaDestino, sesionActual, valorAE);
		this.setProcessedRows("DONE");
	}
	
	
	


	private String obtenerValorAEstandarizar(final AdaptationHome dataspace, final AdaptationTable tabla,String xPath,String parameterxPath, String valuexPath,Path columnaOrigen ,final Session sesionActual) throws OperationException
	{
		 // xPath is ./ID=$param
		  
		 XPathFilter xpf = XPathFilter.newFilter(xPath);

		 Request request = tabla.createRequest();
		 request.setXPathFilter(xpf);
		 request.setXPathParameter(parameterxPath, valuexPath);
		  

		 RequestResult rs = request.execute();
		 
		 
		 Adaptation record=rs.nextAdaptation();
		 
		 Envoltorio envoltorio = new Envoltorio();//Necesario para sacar el valor del procedure sin complicaciones
		  
		  ProgrammaticService service = ProgrammaticService.createForSession(sesionActual, dataspace);
		  ProcedureResult result = service.execute(new  Procedure() 
			{
			    @Override	
				public void execute( ProcedureContext procedureContext) throws Exception 
			    {
			    	ValueContextForUpdate vcfu=procedureContext.getContext(record.getAdaptationName());
			        envoltorio.setValue((String)vcfu.getValue(columnaOrigen));
			       
			    }
				
			});
			
			if(result.hasFailed())
			{
				throw result.getException();
			}
			

		 
		return (String)envoltorio.getValue();
	}

	
	private  void generarTransaccionLLM(final AdaptationHome dataspace, final AdaptationTable tabla,Path columnaDestino, final Session sesionActual, String valorAestandarizar) throws OperationException
	{
		
		final String respuestallm = obtenerRespuestaLLMConcreta(valorAestandarizar, this.prompt); //Cambio reciente
		final ProgrammaticService service = ProgrammaticService.createForSession(sesionActual, dataspace);
		final ProcedureResult result = service.execute(new  Procedure() 
		{
		    @Override	
			public void execute(final ProcedureContext procedureContext) throws Exception 
		    {
		    
		    	ValueContextForUpdate vcfu = procedureContext.getContextForNewOccurrence(tabla);
		    	vcfu.setValue(respuestallm,columnaDestino);
		    	procedureContext.doCreateOccurrence(vcfu, tabla);
		    	
		    }
			
		});
		
		if(result.hasFailed())
		{
			throw result.getException();
		}
		
		
	}
	
	
	public String obtenerRespuestaLLM(String valorAE ,String prompt )
	{
		
		
        String Description = prompt;
        String valorAEstandarizar= valorAE;
        String Prompt= "Realiza lo siguiente:"+Description+"con el siguiente valor:"+valorAEstandarizar;
        StandardizationRequest nameRequest = new StandardizationRequest(
                Prompt, StandardizationTaskType.OTHER
        );
       return StandardizationService.standardize(nameRequest);
	   	
	}
	
	public String obtenerRespuestaLLMConcreta(String valorAE, String prompt)
	{
		 String Description = prompt;
	     String valorAEstandarizar= valorAE;
	    // String Prompt= "Realiza lo siguiente:"+Description+"con el siguiente valor:"+valorAEstandarizar; 
	     String Prompt = Description + " Valor: " + valorAEstandarizar + ". Devuelve el valor estandarizado.";

        StandardizationRequest nameRequest = new StandardizationRequest(
        		this.modeloLLm, Prompt, Integer.parseInt(this.tokens),Double.parseDouble(this.temperatura) , null, false, StandardizationTaskType.OTHER
        );
        return StandardizationService.standardize(nameRequest);
	}
	
	
	//Metodos para obtener cobertir los datos en objetos de utilidad 

	

	private static AdaptationHome toDataSpace (final Repository repository, final String dataSpaceName) throws OperationException
    {
		try {
		 final HomeKey dataSpaceKey = HomeKey.forBranchName(dataSpaceName);	
		 final AdaptationHome dataspace = repository.lookupHome(dataSpaceKey);
		 
		 if(dataspace == null)
		 {
			 throw new IllegalArgumentException();
		 }
		 return dataspace;
		}catch(Exception e) {
		  e.getStackTrace();
		}
		return null;
		
	}
	
	
	private static Adaptation toDataset (final AdaptationHome dataspace, final String datasetName ) throws OperationException
	{
		try {
			
		 final AdaptationName datasetKey = AdaptationName.forName(datasetName);
		 final Adaptation dataset = dataspace.findAdaptationOrNull(datasetKey);
		 return dataset;
		}catch(Exception e) {e.getStackTrace();}
		
		
		return null;
	}
	
	
	private static AdaptationTable toTable (final Adaptation dataset, final String tablePath)
	{
		try {
			final Path path = Path.parse(tablePath);
			final AdaptationTable tabla = dataset.getTable(path);
			return tabla;
		}catch(Exception e) {e.getStackTrace();}
		
		
		return null;
	}
	

	
}


