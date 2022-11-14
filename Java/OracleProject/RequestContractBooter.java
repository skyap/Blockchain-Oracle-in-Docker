package OracleProject;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthAccounts;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple6;
import org.web3j.tuples.generated.Tuple7;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Convert;

/**
 *
 * @author Amir
 */
public class RequestContractBooter {

    private static String DEPLOYED_REQUEST_ADDRESS = "";
    private static String DEPLOYED_ORACLE_ADDRESS = "";
    private static String ORACLE_MANAGER_ADDRESS = "";
    private static final BigInteger GAS_LIMIT = BigInteger.valueOf(6721975L);
    private static final BigInteger GAS_PRICE = BigInteger.valueOf(20000000000L);
    private static final String DEPLOYED_REQUEST_ADDRESS_FILE = "request.txt";
    private static final String DEPLOYED_CONTRACT_ADDRESS_FILE = "deploy.txt";
    private static final String PTINFO = "ptno.txt";
    private static final int ORACLE_MANAGER_INDEX = 0;
    private static Credentials ORACLE_MANAGER_CREDENTIALS = null;
    private static BufferedReader br;
    private static Web3j web3j;
    private static Web3ClientVersion web3ClientVersion;
    private static TransactionManager transactionManager;
    private static EthAccounts ethAccounts;
    private static List<String> ethAccountList;
    private static List<AccountContainer> accountContainers;
    private static Initializer initializer;
    private static ContractGasProvider contractGasProvider;
    private static SecureRandom secureRandom;
    private static List<String> ptInfo;
    protected static List<Request> requestList = new ArrayList<Request>();

    public RequestContractBooter(List<String> ptInfo, List<Request> reqList) {
        setProductInfo(ptInfo);
        requestList = reqList;
    }

    public void setProductInfo(List<String> _ptInfo) {
        ptInfo = _ptInfo;
    }

    public static String productInfoGenerator(SecureRandom secureRandom) {
        return ptInfo.get(secureRandom.nextInt(ptInfo.size()));
    }

    /*
    Print corresponding account information of Ganache-cli
     */
    private static void printAccountInformation() {

        for (AccountContainer account : accountContainers) {
            System.out.println(account.getAccountAddress() + " "
                    + account.getAccountPrivateKey() + " "
                    + account.getAccountBalance() + " "
                    + account.getAccountToken()
            );
        }
    }

    private static Credentials getPrivateKeyFromAddress(String address) {
        return Credentials.create(address);
    }

    private static void getAccountInformation(
            Web3j web3j,
            EthAccounts ethAccounts,
            Initializer initializer
    ) throws IOException {

        List<String> accountList = ethAccounts.getAccounts();
        List<String> privateKeys = initializer.fetchGanachPrivateKeys();
        ORACLE_MANAGER_ADDRESS = accountList.get(0); //Manager index

        for (int i = 0; i < accountList.size(); i++) {
            String address = accountList.get(i);
            EthGetBalance ethGetBalance = web3j.
                    ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            BigDecimal balance = Convert.fromWei(ethGetBalance.getBalance().toString(), Convert.Unit.ETHER);
            accountContainers.add(new AccountContainer(ethAccounts.getAccounts().get(i),
                    privateKeys.get(i),
                    balance,
                    Convert.Unit.ETHER
            ));
        }
    }

    /*
    * Extract component objects
     */
    public static List<ArrayList> fetchTupleComponents(
            Object tuple,
            int numOfTuples
    ) {
        List myTuple = new ArrayList<>();
        switch (numOfTuples) {
            case 7:
                Tuple7 tuple7 = (Tuple7) tuple;
                myTuple.add((BigInteger) tuple7.component1());
                myTuple.add((BigInteger) tuple7.component2());
                myTuple.add((String) tuple7.component3());
                myTuple.add((boolean) tuple7.component4());
                myTuple.add((String) tuple7.component5());
                myTuple.add((ArrayList) tuple7.component6());
                myTuple.add((ArrayList) tuple7.component7());
                break;
            default:
                System.out.println("Error occured during tuple interpretation.");
        }

        return myTuple;
    }

    public static List<Request> buildRequestStruct(
            List request,
            Request_sol_Request deployedContract
    ) {
        List<Request> requestList = new ArrayList<>();

        BigInteger ids = (BigInteger) request.get(0);
        BigInteger arrivalTimes = (BigInteger) request.get(1);
        String senders = (String) request.get(2);
        boolean isPendings = (boolean) request.get(3);;
        String queries = (String) request.get(4);
        ArrayList<String> address = (ArrayList<String>) request.get(5);
        ArrayList<BigInteger> stakes = (ArrayList<BigInteger>) request.get(6);

        requestList.add(new Request(
                ids,
                arrivalTimes,
                senders,
                isPendings,
                queries,
                address,
                stakes,
                deployedContract
        ));

        return requestList;
    }

    public static void printRequestStruct(Request _request) {
        System.out.println(_request.toString());
    }

    /*
    Read sample data for verfication via API
     */
    public static List<String> fetchPTInformation() {
        BufferedReader bf = null;
        List<String> pts = new ArrayList<>();
        String line = "";
        try {
            bf = new BufferedReader(new FileReader(PTINFO));
            while ((line = bf.readLine()) != null) {
                pts.add(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return pts;
    }

    private static Request_sol_Request loadRequestContract(
            Web3j _web3j,
            TransactionManager _transactionManager,
            ContractGasProvider _contractGasProvider
    ) {
        try {
            br = new BufferedReader(new FileReader(DEPLOYED_REQUEST_ADDRESS_FILE));
            DEPLOYED_REQUEST_ADDRESS = br.readLine();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Request_sol_Request.load(DEPLOYED_REQUEST_ADDRESS,
                _web3j,
                _transactionManager,
                _contractGasProvider
        );
    }

    private static Oracle_sol_Oracle loadOracleContract(
            Web3j _web3j,
            TransactionManager _transactionManager,
            ContractGasProvider _contractGasProvider
    ) {
        try {
            br = new BufferedReader(new FileReader(DEPLOYED_CONTRACT_ADDRESS_FILE));
            DEPLOYED_ORACLE_ADDRESS = br.readLine();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(RequestContractBooter.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(RequestContractBooter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return Oracle_sol_Oracle.load(DEPLOYED_ORACLE_ADDRESS,
                _web3j,
                _transactionManager,
                _contractGasProvider
        );
    }

    public static void printRequestInformation(Request_sol_Request deployedRequest) throws Exception {
        BigInteger requestIndex = deployedRequest.countSubmittedRequests().send();
        for (int i = 0; i < requestIndex.intValue(); i++) {
            Tuple7 myTuple = deployedRequest.activeRequests(BigInteger.valueOf(i)).send();
            List<ArrayList> map = fetchTupleComponents(myTuple, 7);
            List<Request> myRequest = buildRequestStruct(map, deployedRequest);
            for (Request req : myRequest) {
                printRequestStruct(req);
            }
        }

    }

    public static byte[] stringToByte(String input) {
        byte[] byteValue = new byte[32];
        System.arraycopy(input.getBytes(), 0, byteValue, 0, input.getBytes().length);
        return byteValue;
    }

    public static void main(String[] args) throws Exception {
        web3j = Web3j.build(new HttpService());
        accountContainers = new ArrayList<>();
        initializer = new Initializer();
        contractGasProvider = new StaticGasProvider(GAS_PRICE, GAS_LIMIT);
        secureRandom = new SecureRandom();
        ptInfo = fetchPTInformation();
        try {

            web3ClientVersion = web3j.web3ClientVersion().send();
            ethAccounts = web3j.ethAccounts().send();
            ethAccountList = ethAccounts.getAccounts();
            getAccountInformation(web3j, ethAccounts, initializer);
//            printAccountInformation();
            ORACLE_MANAGER_CREDENTIALS = getPrivateKeyFromAddress(
                    accountContainers.get(ORACLE_MANAGER_INDEX).getAccountPrivateKey());

            System.out.println(ORACLE_MANAGER_CREDENTIALS.getAddress());

        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

        String web3jClientVersion = web3ClientVersion.getWeb3ClientVersion();
        System.out.println("Current Web3jClinetVersion is " + web3jClientVersion);

        transactionManager = new RawTransactionManager(
                web3j,
                ORACLE_MANAGER_CREDENTIALS
        );

        Request_sol_Request deployedRequest = loadRequestContract(web3j,
                transactionManager,
                contractGasProvider
        );
        String deployedRequestContractAddress = deployedRequest.getContractAddress();
        System.out.println("The REQUEST contract has been deployed at "
                + deployedRequestContractAddress + " "
                + DEPLOYED_REQUEST_ADDRESS
        );

        Oracle_sol_Oracle deployedOracle = loadOracleContract(web3j,
                transactionManager,
                contractGasProvider
        );
        String deployedOracleContractAddress = deployedOracle.getContractAddress();

        deployedRequest.accessDeployedOracleContract(
                deployedOracleContractAddress
        ).send();
        
        

        int id = 0;
        String req = productInfoGenerator(secureRandom); //"fetch " + id;
        System.out.println(" >>> " + Main.requestList.size());
        TransactionReceipt transactionReceipt = deployedRequest.
                createRequest(stringToByte(req)).
                send();
        System.out.println(
                "A new request has been submitted with id = "
                + " " + req
        );
        id++;
        Thread.sleep(15000);
        while (id < 20) {
//            printRequestInformation(deployedRequest);
            req = productInfoGenerator(secureRandom);//"fetch " + id;
            transactionReceipt = deployedRequest.
                    createRequest(stringToByte(req)).
                    send();
            System.out.println(
                    "A new request has been submitted with id = "
                    + " " + req
            );
            Thread.sleep(15000);
            id++;
        }
    }

}
