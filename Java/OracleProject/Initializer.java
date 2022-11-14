package OracleProject;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

/**
 *
 * @author Amir
 */
public class Initializer {

    private static final String TOKEN = "ETH";
    private static final String ADDRESS_FILENAME = "GanacheCLIAddresses.txt";
    private static final String PRIVATEKEY_FILENAME = "GanacheCLIPrivateKeys.txt";
    private static final String PTINFO = "ptno.txt";
    private static final String AVAILABLE_ACCOUNTS = "Available Accounts";
    private static final String PRIVATE_KEYS = "Private Keys";
    private static final String ORACLER = "Oracler";
    private static final String ETHEREUM_ADDRESS = "0x";
    private static final int ADDRESS_LENGTH = 42; //adding the prefix 0x
    private static final int PRIVATEKEY_LENGTH = 66;

    public Initializer() {
    }

    public static byte[] stringToByte(String input) {
        byte[] byteValue = new byte[32];
        System.arraycopy(input.getBytes(), 0, byteValue, 0, input.getBytes().length);
        return byteValue;
    }

    public List<String> fetchGanachPrivateKeys() {
        BufferedReader bf;
        List<String> privateKeys = new ArrayList<>();
        try {
            bf = new BufferedReader(new FileReader(PRIVATEKEY_FILENAME));
            String _lineAccountPrivateKey;
            while ((_lineAccountPrivateKey = bf.readLine()) != null) {
                System.out.println(_lineAccountPrivateKey);
                int _privateKeyIndex = _lineAccountPrivateKey.indexOf(ETHEREUM_ADDRESS);
                String _accountPrivateKey = _lineAccountPrivateKey.substring(_privateKeyIndex, _privateKeyIndex + PRIVATEKEY_LENGTH);
                privateKeys.add(_accountPrivateKey);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return privateKeys;
    }

    public static List<String> oraclersAddress(
            List<AccountContainer> accounts,
            int num
    ) {
        List<String> candidates = new ArrayList<>();
        int value = accounts.size();
        for (int i = 1; i <= num; i++) {
            candidates.add(accounts.get(i).getAccountAddress());
        }
        return candidates;
    }

    public static Map<TransactionReceipt, String> createOraclers(
            Oracle_sol_Oracle _oracler,
            List<String> oraclerAddresses
    ) {
        Map<TransactionReceipt, String> transactions = new HashMap<>();
        for (int i = 0; i < oraclerAddresses.size(); i++) {
            String oraclerName = ORACLER + i;
            String oraclerAddress = oraclerAddresses.get(i);
            TransactionReceipt transactionReceipt = null;
            try {
                transactionReceipt = _oracler.createOracle(stringToByte(oraclerName),
                        oraclerAddress
                ).send();
            } catch (Exception ex) {
                Logger.getLogger(Initializer.class.getName()).log(Level.SEVERE, null, ex);
            }
            transactions.put(transactionReceipt, oraclerAddress);
        }
        return transactions;
    }
    
    /*
    Read sample data for verfication via API
    */
    public List<String> fetchPTInformation(){
        BufferedReader bf = null;
        List<String> pts = new ArrayList<>();
        String line = "";
        try{
            bf = new BufferedReader(new FileReader(PTINFO));
            while((line = bf.readLine()) != null){
                pts.add(line);
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return pts;
    }

//    public static List<AccountContainer> readGanacheCliAccountInformation(){
//        BufferedReader brAccountAddress;
//        BufferedReader brAccountPrivateKey;
//        List<AccountContainer> accountContainer = new ArrayList<>();
//        try{
//            brAccountAddress = new BufferedReader(new FileReader(ADDRESS_FILENAME));
//            brAccountPrivateKey = new BufferedReader(new FileReader(PRIVATEKEY_FILENAME));
//            String _lineAccountAddress;
//            String _lineAccountPrivateKey;
//
//            while(
//                    (_lineAccountAddress = brAccountAddress.readLine()) != null &&
//                    (_lineAccountPrivateKey = brAccountPrivateKey.readLine()) != null
//                ){
//                
//                int _addressIndex = _lineAccountAddress.indexOf(ETHEREUM_ADDRESS);
//                int _privateKeyIndex = _lineAccountPrivateKey.indexOf(ETHEREUM_ADDRESS);
//                
//                String _accountAddress = _lineAccountAddress.substring(_addressIndex, _addressIndex + ADDRESS_LENGTH);
//                String _accountPrivateKey = _lineAccountPrivateKey.substring(_privateKeyIndex, _privateKeyIndex + PRIVATEKEY_LENGTH);
//                double _accountBalance = Double.parseDouble(
//                        _lineAccountAddress.substring(
//                        _addressIndex + ADDRESS_LENGTH, _lineAccountAddress.length()
//                        ).replaceAll("[^\\.0123456789]","")
//                );
//                
//                accountContainer.add(new AccountContainer(
//                        _accountAddress,
//                        _accountPrivateKey,
//                        _accountBalance,
//                        TOKEN
//                ));
//                
//            }
//        }catch(FileNotFoundException ex){
//            ex.printStackTrace();
//        } catch (IOException ex) {
//            Logger.getLogger(Initializer.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        
//        return accountContainer;
//    }
}
