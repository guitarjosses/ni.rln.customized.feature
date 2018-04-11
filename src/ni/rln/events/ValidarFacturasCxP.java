package ni.rln.events;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInvoice;
import org.compiere.model.MPaymentAllocate;
import org.compiere.model.PO;
import org.compiere.util.DB;
import org.osgi.service.event.Event;


public class ValidarFacturasCxP extends AbstractEventHandler{

	@Override
	protected void doHandleEvent(Event event) {
		// TODO Auto-generated method stub
		
		PO po = getPO(event);
		
		MInvoice i = (MInvoice)po;
		
		String sql = "SELECT 1 FROM adempiere.C_PaymentAllocate as pa " +
					 "INNER JOIN adempiere.C_Payment as p " + 
					 "ON p.C_Payment_ID = pa.C_Payment_ID " + 
					 "WHERE pa.C_Invoice_ID=? AND p.DocStatus = 'CO';";
		
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		boolean facturaTienePagos = false;
		
		try{
			
			pstmt = DB.prepareStatement(sql,null);
			pstmt.setInt(1,i.getC_Invoice_ID());
			rs = pstmt.executeQuery();
			
			if(rs.next())
				facturaTienePagos = true;
			
	
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
		
		if(facturaTienePagos)
			throw new AdempiereException("No se puede reversar la factura porque tiene uno o mas pagos asignados");		
		
		


		
	}

	@Override
	protected void initialize() {
		// TODO Auto-generated method stub
		registerTableEvent(IEventTopics.DOC_BEFORE_REVERSECORRECT, MInvoice.Table_Name);
		
		
	}

}
