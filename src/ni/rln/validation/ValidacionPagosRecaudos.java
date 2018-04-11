package ni.rln.validation;

import java.math.BigDecimal;

import org.compiere.model.MClient;
import org.compiere.model.MPayment;
import org.compiere.model.MPaymentAllocate;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;

public class ValidacionPagosRecaudos implements ModelValidator {

	int Cargo;
	boolean esAnticipo;
	boolean hayFactura;
	BigDecimal MontoPago;
	
	@Override
	public void initialize(ModelValidationEngine engine, MClient client) {
		// TODO Auto-generated method stub

		
		engine.addDocValidate(MPayment.Table_Name, this);
		
	}

	@Override
	public int getAD_Client_ID() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String modelChange(PO po, int type) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String docValidate(PO po, int timing) {
		// TODO Auto-generated method stub
		
		Cargo = 0;
		esAnticipo = false;
		hayFactura = false;
		MontoPago =BigDecimal.ZERO;
		
		if(po.get_TableName().equals(MPayment.Table_Name)){
			if(timing==TIMING_BEFORE_COMPLETE){
				MPayment p = (MPayment)po;
				MPaymentAllocate[] pa = MPaymentAllocate.get(p);
				
				Cargo = p.getC_Charge_ID();
				esAnticipo = p.isPrepayment();
				
				if(pa.length > 0)
					hayFactura = true;
				
				if(Cargo == 0 && !esAnticipo && !hayFactura){
					
					return "No se completo el documento, debe seleccionar al menos un cargo, marcar si es anticipo o seleccionar una factura en Pagar Varias Facturas.";
				}
				
				MontoPago = p.getPayAmt();
				
				if(MontoPago.compareTo(BigDecimal.ZERO) == 0)
					return "Debe ingresar un monto mayor a cero";
				
				
			}
		}
		return null;
	}

}
