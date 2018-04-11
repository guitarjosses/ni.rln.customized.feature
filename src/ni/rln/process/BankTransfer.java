package ni.rln.process;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.compiere.model.MBankAccount;
import org.compiere.model.MJournal;
import org.compiere.model.MJournalLine;
import org.compiere.model.MPayment;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

public class BankTransfer extends SvrProcess {

	private String 		p_DocumentNo= "";				// Document No
	private String 		p_Description= "";				// Description
	private int 		p_C_BPartner_ID = 0;   			// Business Partner to be used as bridge
	private int			p_C_Currency_ID = 0;			// Payment Currency
	private int 		p_C_ConversionType_ID = 0;		// Payment Conversion Type
	private int			p_C_Charge_ID = 0;				// Charge to be used as bridge

	private BigDecimal 	p_Amount = Env.ZERO;  			// Amount to be transfered between the accounts
	private int 		p_From_C_BankAccount_ID = 0;	// Bank Account From
	private int 		p_To_C_BankAccount_ID= 0;		// Bank Account To
	private Timestamp	p_StatementDate = null;  		// Date Statement
	private Timestamp	p_DateAcct = null;  			// Date Account
	private int         p_AD_Org_ID = 0;
	private int         m_created = 0;
	private BigDecimal p_tasa_preferencial = Env.ZERO;
	
	private BigDecimal v_monto_tasa_preferencial = Env.ZERO;
	
	@Override
	protected void prepare() {
		// TODO Auto-generated method stub
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (name.equals("From_C_BankAccount_ID"))
				p_From_C_BankAccount_ID = para[i].getParameterAsInt();
			else if (name.equals("To_C_BankAccount_ID"))
				p_To_C_BankAccount_ID = para[i].getParameterAsInt();
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para[i].getParameterAsInt();
			else if (name.equals("C_Currency_ID"))
				p_C_Currency_ID = para[i].getParameterAsInt();
			else if (name.equals("C_ConversionType_ID"))
				p_C_ConversionType_ID = para[i].getParameterAsInt();
			else if (name.equals("C_Charge_ID"))
				p_C_Charge_ID = para[i].getParameterAsInt();
			else if (name.equals("DocumentNo"))
				p_DocumentNo = (String)para[i].getParameter();
			else if (name.equals("Amount"))
				p_Amount = ((BigDecimal)para[i].getParameter());	
			else if (name.equals("Description"))
				p_Description = (String)para[i].getParameter();
			else if (name.equals("StatementDate"))
				p_StatementDate = (Timestamp)para[i].getParameter();
			else if (name.equals("DateAcct"))
				p_DateAcct = (Timestamp)para[i].getParameter();
			else if (name.equals("AD_Org_ID"))
				p_AD_Org_ID = para[i].getParameterAsInt();
			else if (name.equals("tasa_preferencial"))
				p_tasa_preferencial = ((BigDecimal)para[i].getParameter());			
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}		
		
		
	}

	@Override
	protected String doIt() throws Exception {
		// TODO Auto-generated method stub
		if (log.isLoggable(Level.INFO)) log.info("From Bank="+p_From_C_BankAccount_ID+" - To Bank="+p_To_C_BankAccount_ID
				+ " - C_BPartner_ID="+p_C_BPartner_ID+"- C_Charge_ID= "+p_C_Charge_ID+" - Amount="+p_Amount+" - DocumentNo="+p_DocumentNo
				+ " - Description="+p_Description+ " - Statement Date="+p_StatementDate+
				" - Date Account="+p_DateAcct);

		if (p_To_C_BankAccount_ID == 0 || p_From_C_BankAccount_ID == 0)
			throw new AdempiereUserError (Msg.parseTranslation(getCtx(), "@FillMandatory@: @To_C_BankAccount_ID@, @From_C_BankAccount_ID@"));

		if (p_To_C_BankAccount_ID == p_From_C_BankAccount_ID)
			throw new AdempiereUserError (Msg.getMsg(getCtx(), "BankFromToMustDiffer"));
		
		if (p_C_BPartner_ID == 0)
			throw new AdempiereUserError (Msg.parseTranslation(getCtx(), "@FillMandatory@ @C_BPartner_ID@"));
		
		if (p_C_Currency_ID == 0)
			throw new AdempiereUserError (Msg.parseTranslation(getCtx(), "@FillMandatory@ @C_Currency_ID@"));
		
		if (p_C_Charge_ID == 0)
			throw new AdempiereUserError (Msg.parseTranslation(getCtx(), "@FillMandatory@ @C_Charge_ID@"));
	
		if (p_Amount.signum() == 0)
			throw new AdempiereUserError (Msg.parseTranslation(getCtx(), "@FillMandatory@ @Amount@"));

		if (p_AD_Org_ID == 0)
			throw new AdempiereUserError (Msg.parseTranslation(getCtx(), "@FillMandatory@ @AD_Org_ID@"));

		//	Login Date
		if (p_StatementDate == null)
			p_StatementDate = Env.getContextAsDate(getCtx(), "#Date");
		if (p_StatementDate == null)
			p_StatementDate = new Timestamp(System.currentTimeMillis());			

		if (p_DateAcct == null)
			p_DateAcct = p_StatementDate;

		generateBankTransfer();
		return "@Created@ = " + m_created;
	}
	
	/**
	 * Generate BankTransfer()
	 *
	 */
	private void generateBankTransfer()
	{

		MBankAccount mBankFrom = new MBankAccount(getCtx(),p_From_C_BankAccount_ID, get_TrxName());
		MBankAccount mBankTo = new MBankAccount(getCtx(),p_To_C_BankAccount_ID, get_TrxName());
		
		MJournal mNota = new MJournal(getCtx(), 0 ,  get_TrxName());
		MJournalLine mLineas[] = new MJournalLine[2];
		
		mLineas[0] = new MJournalLine(mNota);
		mLineas[1] = new MJournalLine(mNota);
		
		v_monto_tasa_preferencial = this.p_tasa_preferencial.multiply(this.p_Amount) ;
		
		MPayment paymentBankFrom = new MPayment(getCtx(), 0 ,  get_TrxName());
		paymentBankFrom.setC_BankAccount_ID(mBankFrom.getC_BankAccount_ID());
		paymentBankFrom.setAD_Org_ID(p_AD_Org_ID);
		if (!Util.isEmpty(p_DocumentNo, true))
			paymentBankFrom.setDocumentNo(p_DocumentNo);
		paymentBankFrom.setDateAcct(p_DateAcct);
		paymentBankFrom.setDateTrx(p_StatementDate);
		paymentBankFrom.setTenderType(MPayment.TENDERTYPE_DirectDeposit);
		paymentBankFrom.setDescription(p_Description);
		paymentBankFrom.setC_BPartner_ID (p_C_BPartner_ID);
		paymentBankFrom.setC_Currency_ID(p_C_Currency_ID);
		if (p_C_ConversionType_ID > 0)
			paymentBankFrom.setC_ConversionType_ID(p_C_ConversionType_ID);	
		paymentBankFrom.setPayAmt(p_Amount);
		paymentBankFrom.setOverUnderAmt(Env.ZERO);
		paymentBankFrom.setC_DocType_ID(false);
		paymentBankFrom.setC_Charge_ID(p_C_Charge_ID);
		paymentBankFrom.saveEx();
		if(!paymentBankFrom.processIt(MPayment.DOCACTION_Complete)) {
			log.warning("Payment Process Failed: " + paymentBankFrom + " - " + paymentBankFrom.getProcessMsg());
			throw new IllegalStateException("Payment Process Failed: " + paymentBankFrom + " - " + paymentBankFrom.getProcessMsg());
		}
		paymentBankFrom.saveEx();
		addBufferLog(paymentBankFrom.getC_Payment_ID(), paymentBankFrom.getDateTrx(),
				null, paymentBankFrom.getC_DocType().getName() + " " + paymentBankFrom.getDocumentNo(),
				MPayment.Table_ID, paymentBankFrom.getC_Payment_ID());
		m_created++;

		MPayment paymentBankTo = new MPayment(getCtx(), 0 ,  get_TrxName());
		paymentBankTo.setC_BankAccount_ID(mBankTo.getC_BankAccount_ID());
		paymentBankTo.setAD_Org_ID(p_AD_Org_ID);
		if (!Util.isEmpty(p_DocumentNo, true))
			paymentBankTo.setDocumentNo(p_DocumentNo);
		paymentBankTo.setDateAcct(p_DateAcct);
		paymentBankTo.setDateTrx(p_StatementDate);
		paymentBankTo.setTenderType(MPayment.TENDERTYPE_DirectDeposit);
		paymentBankTo.setDescription(p_Description);
		paymentBankTo.setC_BPartner_ID (p_C_BPartner_ID);
		paymentBankTo.setC_Currency_ID(p_C_Currency_ID);
		if (p_C_ConversionType_ID > 0)
			paymentBankTo.setC_ConversionType_ID(p_C_ConversionType_ID);	
		paymentBankTo.setPayAmt(p_Amount);
		paymentBankTo.setOverUnderAmt(Env.ZERO);
		paymentBankTo.setC_DocType_ID(true);
		paymentBankTo.setC_Charge_ID(p_C_Charge_ID);
		paymentBankTo.saveEx();
		if (!paymentBankTo.processIt(MPayment.DOCACTION_Complete)) {
			log.warning("Payment Process Failed: " + paymentBankTo + " - " + paymentBankTo.getProcessMsg());
			throw new IllegalStateException("Payment Process Failed: " + paymentBankTo + " - " + paymentBankTo.getProcessMsg());
		}
		paymentBankTo.saveEx();
		addBufferLog(paymentBankTo.getC_Payment_ID(), paymentBankTo.getDateTrx(),
				null, paymentBankTo.getC_DocType().getName() + " " + paymentBankTo.getDocumentNo(),
				MPayment.Table_ID, paymentBankTo.getC_Payment_ID());
		m_created++;
		return;

	}  //  generateBankTransfer	

}
