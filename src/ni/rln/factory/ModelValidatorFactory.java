package ni.rln.factory;

import ni.rln.validation.ValidacionPagosRecaudos;

import org.adempiere.base.IModelValidatorFactory;
import org.compiere.model.ModelValidator;

public class ModelValidatorFactory implements IModelValidatorFactory {

	@Override
	public ModelValidator newModelValidatorInstance(String className) {
		// TODO Auto-generated method stub
		
		if(className.equalsIgnoreCase( "ni.rln.validation.ValidacionPagosRecaudos"))
			return new ValidacionPagosRecaudos();
		
		return null;
	}

}
