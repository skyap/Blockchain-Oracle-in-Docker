package OracleProject;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

/**
 *
 * @author Amir
 */
public class Oracler {

    private byte[] name;
    private String address;
    private boolean status;
    private BigInteger popularity;
    private BigInteger balance;
    private BigInteger id;
    private Oracle_sol_Oracle contract;
    private Web3j web3j;

    public Oracler(
            byte[] _name,
            String _address,
            BigInteger _popularity,
            boolean _status,
            BigInteger _balance,
            BigInteger _id,
            Oracle_sol_Oracle _contract,
            Web3j _web3j
    ) {
        setOraclerName(_name);
        setOraclerAddress(_address);
        setOraclerPopularity(_popularity);
        setOraclerStatus(_status);
        setOraclerBalance(_balance);
        setOraclerId(_id);
        setContract(_contract);
        setWeb3j(_web3j);
    }

    public Map<Boolean, TransactionReceipt> deactivateOracler() {
        Map<Boolean, TransactionReceipt> result = new HashMap<>();
        boolean response = false;
        TransactionReceipt transactionReceipt = null;
        try {
            transactionReceipt = this.contract.
                    deactivateOraclers(this.address)
                    .send();
            response = true;
            result.put(response, transactionReceipt);
            return result;
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        result.put(response, transactionReceipt);
        return result;
    }

    public Map<Boolean, TransactionReceipt> activateOracler() {
        Map<Boolean, TransactionReceipt> result = new HashMap<>();
        boolean response = false;
        TransactionReceipt transactionReceipt = null;
        try {
            transactionReceipt = this.contract.
                    activateOraclers(this.address)
                    .send();
            response = true;
            result.put(response, transactionReceipt);

        } catch (RuntimeException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        result.put(response, transactionReceipt);
        return result;
    }

    public Map<Boolean, TransactionReceipt> deleteOracler() {
        Map<Boolean, TransactionReceipt> result = new HashMap<>();
        boolean response = false;
        TransactionReceipt transactionReceipt = null;
        try {
            transactionReceipt = this.contract.
                    deleteOraclers(this.address)
                    .send();
            response = true;
            result.put(response, transactionReceipt);
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        result.put(response, transactionReceipt);
        return result;
    }

    public Map<Boolean, TransactionReceipt> changeOraclerPopularity(BigInteger value, BigInteger index) {
        Map<Boolean, TransactionReceipt> result = new HashMap<>();
        boolean response = false;
        TransactionReceipt transactionReceipt = null;
        try {
            transactionReceipt = this.contract.
                    changeOraclerPopularity(this.address,index,value, value)
                    .send();
            response = true;
            result.put(response, transactionReceipt);
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        result.put(response, transactionReceipt);
        return result;
    }

    public List getMappingOraclersIndexes() {
        List<BigInteger> oraclerIndex = null;
        try {
            oraclerIndex = this.contract.
                    mappingOraclersIndex()
                    .send();
            return oraclerIndex;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return oraclerIndex;
    }

    private BigDecimal convertToEther() throws IOException {
        EthGetBalance ethGetBalance = this.web3j.
        ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        BigDecimal balance = Convert.fromWei(ethGetBalance.getBalance().toString(), Convert.Unit.ETHER);
        return balance;
    }
    
    public void setWeb3j(Web3j _web3j){
        this.web3j =  _web3j;
    }
    
    public void setContract(Oracle_sol_Oracle _contract) {
        this.contract = _contract;
    }

    public void setOraclerName(byte[] _name) {
        this.name = _name;
    }

    public void setOraclerAddress(String _address) {
        this.address = _address;
    }

    public void setOraclerStatus(boolean _status) {
        this.status = _status;
    }

    public void setOraclerPopularity(BigInteger _popularity) {
        this.popularity = _popularity;
    }

    public void setOraclerBalance(BigInteger _balance) {
        this.balance = _balance;
    }

    public void setOraclerId(BigInteger _id) {
        this.id = _id;
    }
    
    public Web3j getWeb3j(){
        return this.web3j;
    }

    public byte[] getOraclerName() {
        return this.name;
    }

    public String getOraclerAddress() {
        return this.address;
    }

    public boolean getOraclerStatus() {
        return this.status;
    }

    public BigInteger getOraclerPopularity() {
        return this.popularity;
    }

    public BigInteger getOraclerBalance() {
        return this.balance;
    }

    public BigInteger getOraclerId() {
        return this.id;
    }

    public Oracle_sol_Oracle getContract() {
        return this.contract;
    }

    @Override
    public String toString() {
        String reply = null;
        reply = new String(this.name) + " [" + this.name + "]" + " "
                + this.address + " "
                + this.status + " "
                + this.popularity + " "
                + this.balance + "["
                + Convert.fromWei(this.balance.toString(), Convert.Unit.ETHER) + "] "
                + this.id;
        return reply;
    }

}
