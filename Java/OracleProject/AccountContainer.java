package OracleProject;
import java.math.BigDecimal;
import org.web3j.utils.Convert.Unit;

/**
 *
 * @author Amir
 */
public class AccountContainer {
    private String accountAddress;
    private String accountPrivateKey;
    private BigDecimal accountBalance;
    private Unit accountToken;
    
    public AccountContainer(String _address,String _privateKey, BigDecimal _balance, Unit _token){
        setAccountAddress(_address);
        setAccountPrivateKey(_privateKey);
        setAccountBalance(_balance);
        setAccountToken(_token);
    }
    
    public void setAccountAddress(String _address){
        this.accountAddress = _address;
    }
    
    public void setAccountPrivateKey(String _privatKey){
        this.accountPrivateKey = _privatKey;
    }
    
    public void setAccountBalance(BigDecimal _balance){
        this.accountBalance = _balance;
    }
    
    public void setAccountToken(Unit _name){
        this.accountToken = _name;
    }
    
    public Unit getAccountToken(){
        return this.accountToken;
    }
    
    public String getAccountAddress(){
        return this.accountAddress;
    }
    
    public String getAccountPrivateKey(){
        return this.accountPrivateKey;
    }
    
    public BigDecimal getAccountBalance(){
        return this.accountBalance;
    }

    
}
