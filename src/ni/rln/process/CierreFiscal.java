package ni.rln.process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

import org.compiere.model.MAccount;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MConversionType;
import org.compiere.model.MDocType;
import org.compiere.model.MFactAcct;
import org.compiere.model.MJournal;
import org.compiere.model.MJournalLine;
import org.compiere.model.MPeriod;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

public class CierreFiscal extends SvrProcess{
	
	private int p_C_Period_ID;
	private Timestamp v_FechaFinal;
	private Timestamp v_FechaInicial;
	private Date fecha;
	private String v_Description = null;
	
	private MAcctSchema as = new MAcctSchema(getCtx(),1000000,get_TrxName());

	@Override
	protected void prepare() {
		// TODO Auto-generated method stub
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (name.equals("p_C_Period_ID")) {
				p_C_Period_ID = para[i].getParameterAsInt();				
				}
			else
				log.log(Level.SEVERE, "Unknown Parameter: " + name);			
			}
		
	}

	@Override
	protected String doIt() throws Exception {
		// TODO Auto-generated method stub
		
		SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
		
		if(p_C_Period_ID > 0){
			MPeriod p = new MPeriod(getCtx(),p_C_Period_ID,get_TrxName());	
			v_FechaFinal = p.getEndDate();
			fecha = formato.parse(p.getC_Year().getFiscalYear() + "-01-01");
			v_FechaInicial = Timestamp.valueOf(p.getC_Year().getFiscalYear() + "-01-01 00:00:00.000");
			v_Description = "CIERRE FISCAL " + p.getC_Year().getFiscalYear();

		}
		
		MJournal j = new MJournal(getCtx(),0,get_TrxName());
		
		//System.out.println(Env.getAD_Org_ID(getCtx()));
		
		j.setAD_Org_ID(1000000);
		j.setC_Currency_ID(as.getC_Currency_ID());
		j.setC_DocType_ID(MDocType.getDocType(MDocType.DOCBASETYPE_GLJournal));
		j.setControlAmt(Env.ZERO);
		j.setDateAcct((Timestamp) v_FechaFinal);
		j.setC_Period_ID(p_C_Period_ID);
		j.setDateDoc((Timestamp) v_FechaFinal);
		j.setDescription(v_Description);
		j.setGL_Category_ID(1000012);
		j.setPostingType(MJournal.POSTINGTYPE_Actual);
		j.setC_AcctSchema_ID(as.getC_AcctSchema_ID());
		j.setC_ConversionType_ID(MConversionType.getDefault(Env.getAD_Client_ID(getCtx())));
		j.saveEx();
		
		String sql = null;
		
		sql = "SELECT fa.account_id, " +
			  "CASE (SELECT accounttype FROM adempiere.c_elementvalue " +
				"WHERE c_elementvalue_id = fa.account_id) " + 
			      "WHEN 'R' THEN SUM(fa.amtacctdr-fa.amtacctcr)*-1 " +
                  "ELSE SUM(fa.amtacctdr-fa.amtacctcr) END  AS saldo, " +
			  "(SELECT value FROM adempiere.c_elementvalue " + 
              "WHERE c_elementvalue_id = fa.account_id) as cuenta, " +
              "(SELECT accounttype FROM adempiere.c_elementvalue " +
              "WHERE c_elementvalue_id = fa.account_id) as tipo_cuenta " +
              "FROM adempiere.fact_acct AS fa " +
              "WHERE account_id IN( " +
              "SELECT c_elementvalue_id " + 
              "FROM adempiere.c_elementvalue as ev " +
              "WHERE accounttype IN('R','C','E') " +
              "AND issummary = 'N' " +
              "AND ad_client_id = 1000000 " +
              "ORDER BY value ASC" +
              ") " +
              "AND dateacct BETWEEN ? AND ? " +
              "GROUP BY account_id " +
              "ORDER BY cuenta;";
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try{
			ps = DB.prepareStatement(sql, get_TrxName());
			ps.setTimestamp(1,v_FechaInicial);
			ps.setTimestamp(2,v_FechaFinal);
			rs = ps.executeQuery();
			
			int linea = 0;
			
			while(rs.next()){
				
				linea = linea +10;
				
					System.out.println(rs.getBigDecimal(2));
	
						MJournalLine jl = new MJournalLine(j);
						jl.setAD_Org_ID(1000000);
						jl.setC_BPartner_ID(1000062);
						if(rs.getString(4).equals("R")){
							jl.setAmtAcctDr(rs.getBigDecimal(2));
							jl.setAmtSourceDr(rs.getBigDecimal(2));
						}else{
							jl.setAmtAcctCr(rs.getBigDecimal(2));
							jl.setAmtSourceCr(rs.getBigDecimal(2));
						}
						jl.setAccount_ID(rs.getInt(1));
						jl.setDateAcct(this.v_FechaFinal);
						jl.setC_Project_ID(1000000);
						jl.setC_UOM_ID(100);
						jl.setQty(BigDecimal.ONE);
						jl.setLine(linea);
						jl.setC_Currency_ID(209);
						jl.setC_BPartner_ID(1000062);
						jl.setIsActive(true);
						jl.setIsGenerated(true);
						jl.saveEx();
						
	
			}
			
			
			
			
		}catch(Exception e){
			e.getMessage();
		}
		
		return "CIERRE FISCAL GENERADO SATISFACTORIAMENTE CON DOCUMENTO NUMERO: " + j.getDocumentNo();
	}

}
