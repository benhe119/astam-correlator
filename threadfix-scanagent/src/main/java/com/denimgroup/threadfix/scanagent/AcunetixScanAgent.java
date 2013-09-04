package com.denimgroup.threadfix.scanagent;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import com.denimgroup.threadfix.data.entities.TaskConfig;

public class AcunetixScanAgent extends AbstractScanAgent {
	
	static final Logger log = Logger.getLogger(AcunetixScanAgent.class);
	private String acunetixExecutablePath;

	@Override
	public boolean readConfig(Configuration config) {
		boolean retVal = false;
		
		this.acunetixExecutablePath = config.getString("acunetix.executablePath");
		
		//	TODO - Perform some input validation on the supplied properties so this retVal means something
		retVal = true;
		
		return(retVal);
	}

	@Override
	public File doTask(TaskConfig config) {
		
		File retVal = null;
		
		log.info("Setting up command-line arguments for Acunetix scan");
		
		String acunetixExecutable = this.acunetixExecutablePath + "wvs_console.exe";
		log.debug("Acunetix executable located at: " + acunetixExecutable);
		String targetSite = config.getTargetUrlString();
		log.debug("Site to scan: " + targetSite);
		
		String[] args = { acunetixExecutable, "/scan", targetSite, "/exportxml" };
		
		log.debug("Going to attempt to run ZAP executable at: " + args[0]);
		
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.directory(new File(this.getWorkDir()));
		pb.redirectErrorStream(true);
		
		try {
			Process p = pb.start();
			log.info("Acunetix started successfully.");
			
			InputStreamReader isr = new InputStreamReader(p.getInputStream());
			BufferedReader br = new BufferedReader(isr);
			
			String lineRead;
			while((lineRead = br.readLine()) != null) {
				log.debug("Acunetix out >>>" + lineRead);
			}
			
			int returnCode = p.waitFor();
			log.info("Acunetix process finished with exit code: " + returnCode);
			
			String resultsFilename = this.getWorkDir() + File.separator + "export.xml";
			retVal = new File(resultsFilename);
			
		} catch (IOException e) {
			log.error("Problems starting Acunetix instance: " + e.getMessage(), e);
		} catch (InterruptedException e) {
			log.error("Problems waiting for Acunetix to finish: " + e.getMessage(), e);
		}
		
		return(retVal);
	}

}