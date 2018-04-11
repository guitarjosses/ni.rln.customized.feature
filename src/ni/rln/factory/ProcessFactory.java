package ni.rln.factory;

import ni.rln.process.CierreFiscal;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

public class ProcessFactory implements IProcessFactory {

	@Override
	public ProcessCall newProcessInstance(String className) {
		// TODO Auto-generated method stub
		if(className.equals("ni.rln.process.CierreFiscal"))
		return new CierreFiscal();
		
		return null;
	}

}
